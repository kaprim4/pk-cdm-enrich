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
 * Traitement OCR Lot
 * @author K.ABIDA
 *
 */
public class LotWorkOCR {
	/**
	 * Objet qui contient toute les information LOT
	 */
	PCDocument documentLot;
	
	/**
	 * le temps de traitement OCR
	 */
	int timeTrt;
	
	/**
	 * Objet qui contient les informations LOT lu par l'OCR
	 */
	public LotData lotData;

	/**
	 * Constrcuteur d'objet
	 * @param document Objet qui contient toute les information LOT
	 */
	public LotWorkOCR(PCDocument document) {
		documentLot = document;
		timeTrt = -1;
	}

	/**
	 * Fonction de traitement OCR Lot
	 * @return true si ok, false sinon
	 */
	public boolean doWork() {
		boolean isOk = true;
		CommonVars.logger.info("Start LotWorkOCR.doWork");
		try {
			Date startDate = new Date();
			ITesseract instanceColor = new Tesseract();

			instanceColor.setLanguage(CommonVars.srvConf.getTesseractPatam().getLang());

			instanceColor.setDatapath(CommonVars.srvConf.getTesseractPatam().getDirParam());
			instanceColor.setTessVariable("tessedit_char_whitelist",
					CommonVars.srvConf.getTesseractPatam().getWhiteList());
			instanceColor.setTessVariable("tessedit_parallelize", "10");

			ITesseract instanceBW = new Tesseract();
			instanceBW.setLanguage(CommonVars.srvConf.getTesseractPatam().getLang());
			instanceBW.setDatapath(CommonVars.srvConf.getTesseractPatam().getDirParam());
			instanceBW.setTessVariable("tessedit_char_whitelist",
					CommonVars.srvConf.getTesseractPatam().getWhiteList());
			instanceBW.setTessVariable("tessedit_parallelize", "10");

			lotData = new LotData();

			String data = instanceBW
					.doOCR(getCrop(documentLot.getImageBWFront(), CommonVars.srvConf.getTLot().getAgence()));
			lotData.loadData(data, "BW", "CodeAgence");

			data = instanceBW
					.doOCR(getCrop(documentLot.getImageBWFront(), CommonVars.srvConf.getTLot().getReference()));
			lotData.loadData(data, "BW", "reference");

			data = instanceBW.doOCR(getCrop(documentLot.getImageBWFront(), CommonVars.srvConf.getTLot().getNbremise()));
			lotData.loadData(data, "BW", "nbrremise");

			data = instanceBW.doOCR(getCrop(documentLot.getImageBWFront(), CommonVars.srvConf.getTLot().getMontant()));
			lotData.loadData(data, "BW", "montant");

			data = instanceBW.doOCR(getCrop(documentLot.getImageBWFront(), CommonVars.srvConf.getTLot().getDatelot()));
			lotData.loadData(data, "BW", "date");
			/*
			 * CommonVars.logger.info("BW OCR" + docRemise.getFileName() +
			 * lotData.toString()); result = instanceColor
			 * .doOCR(getCrop(docRemise.getImageColorFront(),
			 * CommonVars.srvConf.getTLot().getAgence()));
			 * 
			 * 
			 * lotData.loadData(result, "COLOR", "CodeAgence");
			 */
			CommonVars.logger.info("REMDATAFINAL :" + documentLot.getFileName() + " => " + lotData.toString());
			insertHistoOCR(lotData.agence, lotData.ref, lotData.amount, lotData.dateR, lotData.nbValeur,
					PCHDateFunc.getDiffDateSecFromNow(startDate));

		} catch (Exception e) {
			CommonVars.logger.error("#LotWorkOCR.doWork#", e);
			isOk = false;
		}
		CommonVars.logger.info("End LotWorkOCR.doWork");
		return isOk;
	}

	/**
	 * Recupere le crop à lire de l'image Ticket Lot
	 * @param image l'image lot
	 * @param ocrCrop parametrage du crop à recuperer
	 * @return le crop type BufferedImage
	 */
	public BufferedImage getCrop(byte[] image, OCRCrop ocrCrop) {
		CommonVars.logger.info("Start LotWorkOCR.getCrop");

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
			CommonVars.logger.error("#LotWorkOCR.getCrop#", e);
			CommonVars.logger.info("End LotWorkOCR.getCrop");
			return null;
		}
	}

	/**
	 * insert le resultat de traitement OCR pour le LOT
	 * @param codeAgence agence remettante
	 * @param ref reference lot
	 * @param montant montant lot
	 * @param date la date du lot
	 * @param nbRemise nombre de remise
	 * @param timeScore le temps de traitement OCR
	 */
	private void insertHistoOCR(String codeAgence, String ref, String montant, String date, String nbRemise,
			int timeScore) {
		PreparedStatement ps = null;
		try {
			boolean isOk = CommonVars.connectToDb();
			if (isOk) {
				String insertQuery = "insert into T_HISTO_BATCH_ENRICH_OCR "
						+ "(s_source, s_file_name, s_doc_type, s_data_1, s_data_2, s_data_3,"
						+ "s_data_4, s_data_5, s_data_6, dt_process, n_time_score, s_report ,s_project) "
						+ "values (?,?,?,?,?,?,?,?,?,getdate(),?,?,?)";
				ps = CommonVars.ConDb.prepareStatement(insertQuery);

				ps.setString(1, documentLot.getSource());
				ps.setString(2, documentLot.getFileName());
				ps.setString(3, "TLOT");
				ps.setString(4, documentLot.getSeqDoc());
				ps.setString(5, codeAgence);
				ps.setString(6, ref);
				ps.setString(7, montant);
				ps.setString(8, date);
				ps.setString(9, nbRemise);
				ps.setInt(10, timeScore);
				ps.setString(11, "");
				ps.setString(12, Main.PROJECT_NAME);
				isOk = ps.execute();
			}
		} catch (Exception e) {
			CommonVars.logger.error("#LotWorkOCR.insertHistoOCR#", e);
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
