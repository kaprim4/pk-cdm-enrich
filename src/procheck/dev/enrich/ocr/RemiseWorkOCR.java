package procheck.dev.enrich.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.Date;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.ocr.ServerConfig.OCRCrop;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.lib.PCHDateFunc;

/**
 * Traitement OCR Remise
 * 
 * @author K.ABIDA
 *
 */
public class RemiseWorkOCR {
	/**
	 * Objet qui contient toute les information Remise
	 */
	PCDocument documentRemise;

	/**
	 * le temps de traitement OCR
	 */
	int timeTrt;

	/**
	 * Objet qui contient les informations Remise lu par l'OCR
	 */
	public RemiseData remiseData;

	/**
	 * Constrcuteur d'objet
	 * 
	 * @param document Objet qui contient toute les information remise
	 */
	public RemiseWorkOCR(PCDocument document) {
		documentRemise = document;
		timeTrt = -1;
	}
	/**
	 * Fonction de traitement OCR Lot
	 * @return true si ok, false sinon
	 */
	public boolean doWork() {
		boolean isOk = true;
		CommonVars.logger.info("Start RemiseWorkOCR.doWork");
		try {
			Date startDate = new Date();
			/*
			 * ITesseract instanceColor = new Tesseract();
			 * 
			 * instanceColor.setLanguage(CommonVars.srvConf.getTesseractPatam().getLang());
			 * 
			 * instanceColor.setDatapath(CommonVars.srvConf.getTesseractPatam().getDirParam(
			 * )); instanceColor.setTessVariable("tessedit_char_whitelist",
			 * CommonVars.srvConf.getTesseractPatam().getWhiteList());
			 * instanceColor.setTessVariable("tessedit_parallelize", "10");
			 */
			ITesseract instanceBW = new Tesseract();
			instanceBW.setLanguage(CommonVars.srvConf.getTesseractPatam().getLang());
			instanceBW.setDatapath(CommonVars.srvConf.getTesseractPatam().getDirParam());
			instanceBW.setTessVariable("tessedit_char_whitelist",
					CommonVars.srvConf.getTesseractPatam().getWhiteList());
			instanceBW.setTessVariable("tessedit_parallelize", "10");

			remiseData = new RemiseData();

			String data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getAgence()));
			remiseData.loadData(data, "BW", "CodeAgence");

			data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getReference()));
			remiseData.loadData(data, "BW", "reference");

			data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getNbrCheque()));
			remiseData.loadData(data, "BW", "nbrremise");

			data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getMontant()));
			remiseData.loadData(data, "BW", "montant");

			data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getDate()));
			remiseData.loadData(data, "BW", "date");

			data = instanceBW
					.doOCR(getCrop(documentRemise.getImageBWFront(), CommonVars.srvConf.getTRemise().getNumCompte()));
			remiseData.loadData(data, "BW", "compte");
			/*
			 * CommonVars.logger.info("BW OCR" + docRemise.getFileName() +
			 * lotData.toString()); result = instanceColor
			 * .doOCR(getCrop(docRemise.getImageColorFront(),
			 * CommonVars.srvConf.getTLot().getAgence()));
			 * 
			 * 
			 * lotData.loadData(result, "COLOR", "CodeAgence");
			 */
			CommonVars.logger.info("REMDATAFINAL :" + documentRemise.getFileName() + " => " + remiseData.toString());
			insertHistoOCR(remiseData.agence, remiseData.ref, remiseData.compte, remiseData.amount, remiseData.dateR,
					remiseData.nbValeur, PCHDateFunc.getDiffDateSecFromNow(startDate));

		} catch (Exception e) {
			CommonVars.logger.error("#RemiseWorkOCR.doWork#", e);
			isOk = false;
		}
		CommonVars.logger.info("End RemiseWorkOCR.doWork");
		return isOk;
	}

	/**
	 * Recupere le crop à lire de l'image Ticket remise
	 * @param image l'image remise
	 * @param ocrCrop parametrage du crop à recuperer
	 * @return le crop type BufferedImage
	 */
	public BufferedImage getCrop(byte[] image, OCRCrop ocrCrop) {
		CommonVars.logger.info("Start RemiseWorkOCR.getCrop");

		try {
			InputStream in = new ByteArrayInputStream(image);
			BufferedImage originalImage = ImageIO.read(in);
			CommonVars.logger
					.info("Original Image Dimension: " + originalImage.getWidth() + "x" + originalImage.getHeight());
			BufferedImage cropImage = originalImage.getSubimage((int) (originalImage.getWidth() * ocrCrop.getX()),
					(int) (originalImage.getHeight() * ocrCrop.getY()),
					(int) (originalImage.getWidth() * ocrCrop.getW()),
					(int) (originalImage.getHeight() * ocrCrop.getH()));
			/*
			 * File outputfile = new File("d:\\tfo\\image" + IDX_IDX + ".jpg"); IDX_IDX++;
			 * ImageIO.write(cropImage, "jpg", outputfile);
			 */
			return cropImage;
		} catch (Exception e) {
			CommonVars.logger.error("#RemiseWorkOCR.getCrop#", e);
			CommonVars.logger.info("End RemiseWorkOCR.getCrop");
			return null;
		}
	}

	/**
	 * insert le resultat de traitement OCR pour le ticket remise sur la BD
	 * @param codeAgence agence compte
	 * @param ref reference remise
	 * @param compte numero de compte remettant
	 * @param montant montant de la remise
	 * @param date la date de la remise
	 * @param nbValeurs nombre de valeur dans la remise
	 * @param timeScore  le temps de traitement OCR
	 */
	private void insertHistoOCR(String codeAgence, String ref, String compte, String montant, String date,
			String nbValeurs, int timeScore) {
		PreparedStatement ps = null;
		try {
			boolean isOk = CommonVars.connectToDb();
			if (isOk) {
				String insertQuery = "insert into T_HISTO_BATCH_ENRICH_OCR "
						+ "(s_source, s_file_name, s_doc_type, s_data_1, s_data_2, s_data_3,"
						+ "s_data_4, s_data_5, s_data_6,s_data_7, dt_process, n_time_score, s_report ,s_project) "
						+ "values (?,?,?,?,?,?,?,?,?,?,getdate(),?,?,?)";
				ps = CommonVars.ConDb.prepareStatement(insertQuery);

				ps.setString(1, documentRemise.getSource());
				ps.setString(2, documentRemise.getFileName());
				ps.setString(3, "TREMISE");
				ps.setString(4, documentRemise.getSeqDoc());
				ps.setString(5, codeAgence);
				ps.setString(6, ref);
				ps.setString(7, montant);
				ps.setString(8, date);
				ps.setString(9, nbValeurs);
				ps.setString(10, compte);
				ps.setInt(11, timeScore);
				ps.setString(12, "");
				ps.setString(13, Main.PROJECT_NAME);
				isOk = ps.execute();
			}
		} catch (Exception e) {
			CommonVars.logger.error("#RemiseWorkOCR.insertHistoOCR#", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
