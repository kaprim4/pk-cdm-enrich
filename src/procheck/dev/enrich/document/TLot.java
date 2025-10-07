package procheck.dev.enrich.document;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.freenect2.Logger;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.ocr.LotWorkOCR;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.enrich.tools.ImageTools;
import procheck.dev.lib.PCHStringOpr;
/**
 * un objet qui contient les informations d'un ticket lot BMCE
 * @author K.ABIDA
 *
 */
public class TLot {
	/**
	 * la ligne lot sur le fichier .lot
	 */
	String lineToAnalyse;
	
	/**
	 * les donn�es d'enrichissement pour le lot
	 */
	String dataEnrich;
	
	/**
	 * l'image recto en noir et blanc
	 */
	byte[] imageBWFront;
	
	/**
	 * l'image verso en noir et blanc
	 */
	byte[] imageBWRear;
	
	/**
	 * l'image recto en TG
	 */
	byte[] imageColorFront;
	
	/**
	 * l'image verso en TG
	 */
	byte[] imageColorRear;
	
	/**
	 * specifier si l'image vrituelle � �t� cr�e
	 */
	boolean withNewImage;
	
	/**
	 * contient l'erreur si le traitement genere une erreur
	 */
	String msgError;

	/**
	 * Constructeur de l'objet
	 * @param line la ligne du lot sur le fichier .lot
	 */
	public TLot(String line) {
		lineToAnalyse = line;
		dataEnrich = CommonVars.HEAD_DATA_DOC;
		msgError = "";
	}

	/**
	 * fonction qui traite le lot
	 * @param document un objet qui contient toute les informations du document lot
	 * @param agenceRemIn l'agence remettante
	 * @param dateLot la date de generation des grains
	 * @return true si le traitement est ok false sinon
	 */
	public boolean doWork(PCDocument document, String agenceRemIn, String dateLot) {
		CommonVars.logger.info("TLot.dowork START");
		boolean isOk = true;
		try {
			String agenceRem = "", dataCmc7Lot, reference = "", montant = "", nbRemise = "";

			dataEnrich = CommonVars.insertStringToString(dataEnrich, "01", 11);
			boolean isVirtual = false;
			isOk = !CommonVars.isTLotAlreadyImported(document.getCapturePoint(), lineToAnalyse.substring(9, 73).trim(),dateLot);
			if (!isOk) {
				msgError = "Ticket lot deja import�";
			} else {
				if (document.getLengthRectoBW() == 0 && document.getLengthRectoColor() == 0
						&& document.getLengthVersoBW() == 0 && document.getLengthVersoColor() == 0) {
					agenceRem = agenceRemIn;
					isVirtual = true;
					BufferedImage imageTiff = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "555550000002.tiff"));
					BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "555550000002.jpg"));
					montant = "0.1";
					reference = "00" + lineToAnalyse.substring(14, 21);
					nbRemise = "1";
					isOk = ImageTools.putAllDataInImageLot(imageJPG, agenceRemIn, dateLot);
					if (isOk) {
						isOk = ImageTools.putAllDataInImageLot(imageTiff, agenceRemIn, dateLot);
					}
					if (isOk) {
						this.imageBWFront = ImageTools.compressTiff(imageTiff);
						this.imageBWRear = ImageTools.compressTiff(imageTiff);
						this.imageColorFront = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
						this.imageColorRear = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
						CommonVars.logger.debug(
								"Lot DATA [BW:" + imageBWFront.length + " | COLOR:" + imageColorFront.length + "]");
						isOk = (this.imageBWFront != null && this.imageBWRear != null && this.imageColorFront != null
								&& this.imageColorRear != null);
						withNewImage = true;
					}
				} else {
					LotWorkOCR lotWorkOCR = new LotWorkOCR(document);
					if (lotWorkOCR.doWork()) {
						agenceRem = lotWorkOCR.lotData.getAgence();
						reference = lotWorkOCR.lotData.getRef();
						dateLot = lotWorkOCR.lotData.getDateR();
						montant = lotWorkOCR.lotData.getAmount();
						nbRemise = lotWorkOCR.lotData.getNbValeur();
					}
				}
				dataCmc7Lot = getCmc7Data(lineToAnalyse.substring(9, 73), isVirtual);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dataCmc7Lot, " ", 64),
						13);

				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(agenceRem, " ", 10), 77);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(reference, " ", 10), 87);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(montant, " ", 18), 97);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(nbRemise, " ", 4), 115);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dateLot, " ", 14), 119);
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 250); // Zone Libre
			}
		} catch (Exception e) {
			CommonVars.logger.error("#TLot.doWork#", e);
			msgError = "#TLot.doWork#" + e.getMessage();
			isOk = false;
		}
		return isOk;
	}

	/**
	 * Traitement pour les lot de la capture web
	 * @param document un objet qui contient toute les informations du document lot
	 * @param agenceRemIn l'agence remettante
	 * @param dateLot la date de generation des grains
	 * @return true si le traitement est ok false sinon
	 */
	public boolean doWorkCapWeb(PCDocument document, String agenceRemIn, String dateLot) {
		boolean isOk = true;
		try {
			BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "555550000002.jpg"));
			
			isOk = ImageTools.putAllDataInImageLot(imageJPG, agenceRemIn, dateLot);

			if (isOk) {
				this.imageColorFront = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				this.imageColorRear = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				CommonVars.logger.debug("Lot DATA [BW: COLOR:" + imageColorFront.length + "]");
				isOk = (this.imageColorFront != null && this.imageColorRear != null);
			} else {
				msgError = "#TLot.doWorkCapWeb# can't putAllDataInImageLot ";
			}
			
		} catch (Exception e) {
			CommonVars.logger.error("#TLot.doWorkCapWeb#", e);
			msgError = "#TLot.doWorkCapWeb#" + e.getMessage();
			isOk = false;
		} finally {
			CommonVars.logger.info("end Tlot.doWorkCapWeb [" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * recupere la valeur du cmc7  
	 * @param cmc7Ligne la cmc7 lu par le scanner
	 * @param isVirtual specifie si le ticket lot est virtuel ou non
	 * @return la valeur de CMC7 lot
	 */
	private String getCmc7Data(String cmc7Ligne, boolean isVirtual) {
		String dataRet = "";
		String regex = "(\\d){7};55555(\\d){7}";
		try {
			if (isVirtual) {
				dataRet = cmc7Ligne.substring(5, 12) + ";" + cmc7Ligne.substring(16, 28);
			} else {
				cmc7Ligne = cmc7Ligne.trim().replace(" ", ";");
				if (cmc7Ligne.matches(regex)) {
					dataRet = cmc7Ligne;
				}
			}
		} catch (Exception e) {
			dataRet = "";
		}
		return dataRet;
	}

	public String getDataEnrich() {
		return dataEnrich;
	}

	public String getLineToAnalyse() {
		return lineToAnalyse;
	}

	public byte[] getImageBWFront() {
		return imageBWFront;
	}

	public byte[] getImageBWRear() {
		return imageBWRear;
	}

	public byte[] getImageColorFront() {
		return imageColorFront;
	}

	public byte[] getImageColorRear() {
		return imageColorRear;
	}

	public boolean isWithNewImage() {
		return withNewImage;
	}

	/**
	 * @return the msgError
	 */
	public String getMsgError() {
		return msgError;
	}

}
