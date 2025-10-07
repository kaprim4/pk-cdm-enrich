package procheck.dev.enrich.document;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.ocr.RemiseWorkOCR;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.enrich.tools.ImageTools;
import procheck.dev.lib.PCHStringOpr;
import procheck.dev.lib.ai.PKOCRCMC7;

/**
 * un objet qui contient les informations d'un ticket remise BMCE
 * 
 * @author K.ABIDA
 *
 */
public class TRemise {

	/**
	 * la ligne remise sur le fichier .lot
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
	 * 
	 * @param line la ligne du remise sur le fichier .lot
	 */
	public TRemise(String line) {
		lineToAnalyse = line;
		dataEnrich = CommonVars.HEAD_DATA_DOC;
		withNewImage = false;
		msgError = "";
	}

	/**
	 * fonction qui traite le ticket remise
	 * 
	 * @param document    un objet qui contient toute les informations du document
	 *                    remise
	 * @param agenceRemIn l'agence remettante
	 * @param dateRemise  la date de generation des grains
	 * @param nbrValeur   nombre de valeur dans la remise
	 * @return true si le traitement est ok false sinon
	 */
	public boolean doWork(PCDocument document, String agenceRemIn, String dateRemise, int nbrValeur) {
		boolean isOk = true;
		try {
			String referenceRemise = "", agenceRem = "", montant = "", numCompte = "", typeRemise = null,
					nbValeur = nbrValeur + "", dataCm7cRemise;
			dataEnrich = CommonVars.insertStringToString(dataEnrich, "02", 11);
			if (document.getLengthRectoBW() == 0 && document.getLengthRectoColor() == 0 && document.getLengthVersoBW() == 0 && document.getLengthVersoColor() == 0) {
				agenceRem = agenceRemIn;
				if (lineToAnalyse.substring(36, 46).trim().length() > 9) {
					referenceRemise = lineToAnalyse.substring(36, 46).trim().substring(0, 9);
				} else {
					referenceRemise = lineToAnalyse.substring(36, 46).trim();
				}
				montant = (String.format("%.2f",
						(Double.parseDouble(lineToAnalyse.substring(52, 65).trim().substring(0, 12)) / 100)))
								.replace(".", ",");
				numCompte = lineToAnalyse.substring(19, 22) + lineToAnalyse.substring(22, 36);
				//typeRemise = lineToAnalyse.substring(17, 19);
				typeRemise = lineToAnalyse.substring(875, 876);
				// **************************************************************//
				// ** Creation des images **//
				// **************************************************************//
				BufferedImage imageTiff = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + getTypeRemise(typeRemise) + ".tiff"));
				BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + getTypeRemise(typeRemise) + ".jpg"));
				isOk = ImageTools.putAllDataInImageRemise(imageJPG, lineToAnalyse.substring(19, 24),
						lineToAnalyse.substring(24, 36), referenceRemise, nbrValeur + "", montant, dateRemise, null,
						"");
				if (isOk) {
					isOk = ImageTools.putAllDataInImageRemise(imageTiff, lineToAnalyse.substring(19, 24),
							lineToAnalyse.substring(24, 36), referenceRemise, nbrValeur + "", montant, dateRemise, null,
							"");
				}
				if (isOk) {
					this.imageBWFront = ImageTools.compressTiff(imageTiff);
					this.imageBWRear = ImageTools.compressTiff(imageTiff);
					this.imageColorFront = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
					this.imageColorRear = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
					isOk = (this.imageBWFront != null && this.imageBWRear != null && this.imageColorFront != null
							&& this.imageColorRear != null);
					CommonVars.logger.debug(
							"REMISE DATA [BW:" + imageBWFront.length + " | COLOR:" + imageColorFront.length + "]");
					withNewImage = true;
				}
			} else {
				RemiseWorkOCR remiseWorkOCR = new RemiseWorkOCR(document);
				if (remiseWorkOCR.doWork()) {
					agenceRem = remiseWorkOCR.remiseData.getAgence();
					referenceRemise = remiseWorkOCR.remiseData.getRef();
					montant = remiseWorkOCR.remiseData.getAmount().trim().length() > 0
							? PCHStringOpr.getStringFromDouble(
									Double.parseDouble(remiseWorkOCR.remiseData.getAmount().replace(",", ".")))
							: "";
					nbValeur = remiseWorkOCR.remiseData.getNbValeur();
					dateRemise = remiseWorkOCR.remiseData.getDateR();
					numCompte = (agenceRem.length() == 5 && remiseWorkOCR.remiseData.getCompte().length() == 12
							&& (agenceRem.equals(agenceRemIn) || agenceRemIn.equalsIgnoreCase("00001")))
									? (agenceRem + remiseWorkOCR.remiseData.getCompte())
									: "";
				}
			}
			if (isOk) {
				dataCm7cRemise = getCmc7Data(lineToAnalyse.substring(9, 73), typeRemise);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dataCm7cRemise, " ", 64), 13);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(agenceRem, " ", 10), 77);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(referenceRemise, " ", 10), 87);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(montant, " ", 18), 97);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(nbValeur, " ", 4), 115);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dateRemise, " ", 14), 119);
				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(numCompte, " ", 24), 133);
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 181); // Zone Libre
			}
		} catch (Exception e) {
			CommonVars.logger.error("#TRemise.doWork#", e);
			isOk = false;
		}
		return isOk;
	}

	/**
	 * Traitement pour les remises de la capture web
	 * 
	 * @param document    un objet qui contient toute les informations du document
	 *                    remise
	 * @param agenceRemIn l'agence remettante
	 * @param dateRemise  date de la remise
	 * @param nbrValeur   nombre de valeur
	 * @param typeRemise  type de la remise
	 * @return true si le traitement est ok false sinon
	 */
	public boolean doWorkCapWeb(PCDocument document, String agenceRemIn, String dateRemise, int nbrValeur
			) {
		CommonVars.logger.info("=> Start TRemise.doWorkCapWeb <=");
		boolean isOk = true;
		try {
			String referenceRemise = "", referenceClient ="",agenceRem = "", montant = "", numCompte = "", nomCompte = "";
//			//agenceRem = StringUtils.rightPad(String.valueOf(lineToAnalyse.substring(850, 853)),5 ," ");
//			agenceRem = lineToAnalyse.substring(850, 853);
//			numCompte = lineToAnalyse.substring(850, 862);
//			referenceRemise = lineToAnalyse.substring(806, 814);
//			referenceClient = lineToAnalyse.substring(919, 931).trim();
//			//referenceRemise = PCHStringOpr.leftPad(referenceRemise, "0", 10);
//			nomCompte = lineToAnalyse.substring(881,lineToAnalyse.length() - 34 > 881 ? 881 + 34 : lineToAnalyse.length()).trim();
//			montant = (String.format("%.2f", (Double.parseDouble(lineToAnalyse.substring(814, 832).replace(",", ".").trim())))).replace(".",",");
			
			 //agenceRem = StringUtils.rightPad(String.valueOf(lineToAnalyse.substring(850, 853)),5 ," ");
			   agenceRem = lineToAnalyse.substring(796, 800);
			   numCompte = lineToAnalyse.substring(850, 862);
			   referenceRemise = lineToAnalyse.substring(806, 814);
			   referenceClient = lineToAnalyse.substring(919, 931).trim();
			   //referenceRemise = PCHStringOpr.leftPad(referenceRemise, "0", 10);
			   nomCompte = lineToAnalyse.substring(881,lineToAnalyse.length() - 34 > 881 ? 881 + 34 : lineToAnalyse.length()).trim();
			   montant = (String.format("%.2f", (Double.parseDouble(lineToAnalyse.substring(814, 832).replace(",", ".").trim())))).replace(".",",");
			   
			String typeRemise = lineToAnalyse.substring(875, 876);
			//typeRemise = typeRemise.trim().length()==0 ? "1" : typeRemise;
			CommonVars.logger.info("Type remise : "+typeRemise + " => "+lineToAnalyse.substring(875, 876));
			
			// **************************************************************//
			// ** Creation des images **//
			// **************************************************************//
			
			
			BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + getImgRemiseCaptureWeb(typeRemise) + ".jpg"));
			CommonVars.logger.info("Get Image Remise Virtuelle From : ["+Main.IMAGE_PATH_FOR_VIRTUAL + getImgRemiseCaptureWeb(typeRemise) + ".jpg]");
			isOk = ImageTools.putAllDataInImageRemise(imageJPG, agenceRem, numCompte, referenceRemise, nbrValeur + "", montant, dateRemise, referenceClient, nomCompte);

			if (isOk) {
				this.imageColorFront = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				this.imageColorRear = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");

				isOk = (this.imageColorFront != null && this.imageColorRear != null);
				CommonVars.logger.debug("REMISE DATA [ COLOR:" + imageColorFront.length + "]");
			}
		} catch (Exception e) {
			CommonVars.logger.error("#TRemise.doWorkCapWeb#", e);
			isOk = false;
		} finally {
			CommonVars.logger.info("end TRemise.doWorkCapWeb [" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * recupere la valeur du cmc7
	 * 
	 * @param cmc7Ligne  la cmc7 lu par le scanner
	 * @param typeRemise type de la remise
	 * @return la valeur de CMC7 remise
	 */
	private String getCmc7Data(String cmc7Ligne, String typeRemise) {
		String dataRet = "";
		String regex = "(\\d){7};44444(\\d){7}";
		try {
			if (typeRemise == null) {
				cmc7Ligne = cmc7Ligne.trim().replace(" ", ";");
				if (cmc7Ligne.matches(regex)) {
					dataRet = cmc7Ligne;
				} else {
					// appel CMC7 AI
					PKOCRCMC7 pkOCRCMC7 = new PKOCRCMC7();
					boolean retOCR = pkOCRCMC7.doWork(CommonVars.OCR_AI_URL_REMISE, imageColorFront, "remise");
					CommonVars.logger.info("ICR RETURN : [" + retOCR + "]");
					if (!retOCR) {
						CommonVars.logger.error(pkOCRCMC7.getErrorCode());
					} else {
						cmc7Ligne = pkOCRCMC7.getDataOCR().cmc7.replace('H', ';');
						// recuperer cmc7 sans premiere et derniere H
						if (cmc7Ligne.startsWith(";"))
							cmc7Ligne = cmc7Ligne.substring(1);
						if (cmc7Ligne.endsWith(";"))
							cmc7Ligne = cmc7Ligne.substring(0, cmc7Ligne.length() - 1);
						if (cmc7Ligne.matches(regex)) {
							dataRet = cmc7Ligne;
						}
					}
					CommonVars.logger.info("OCR DATA :[" + cmc7Ligne + "]");
				}
			} else {
				dataRet = cmc7Ligne.substring(0, 7) + ";" + getTypeRemise(typeRemise);
			}
		} catch (Exception e) {
			dataRet = "";
		}
		return dataRet;
	}

	public String getDataEnrich() {
		return dataEnrich;
	}
	
	

	private String getTypeRemise(String typeRemise) {
		String dataRet = "";
		
		 if (typeRemise.equals("1") || typeRemise.equals("3")) {
			 dataRet = "444440000033";
		}
		 else if (typeRemise.equals("2") || typeRemise.equals("4")) {
			 dataRet = "444440000022";
		}
		 else {
			 CommonVars.logger.warn(" Type de remise inconu ... ");
		 }
		 
		/**
		 * if (typeRemise.equals("50")) {
			dataRet = "444440000033";
		} else if (typeRemise.equals("56")) {
			dataRet = "444440000055";
		} else if (typeRemise.equals("57")) {
			dataRet = "444440000033";
		} else if (typeRemise.equals("58")) {
			dataRet = "444440000077";
		} else if (typeRemise.equals("59")) {
			dataRet = "444440000088";
		} else if (typeRemise.equals("51")) {
			dataRet = "444440000044";
		} else {
			dataRet = "444440000033";
		}**/
		return dataRet;
	}

	private String getImgRemiseCaptureWeb(String typeRemise) {
		String dataRet = "";
		if(typeRemise.equals("1")) {
			dataRet = "CHQ-1";
		}else if(typeRemise.equals("2")) {
			dataRet = "CHQ-2";
		}else if(typeRemise.equals("3")) {
			dataRet = "LCN-1";
		}else if(typeRemise.equals("4")) {
			dataRet = "LCN-2";
		} return dataRet;
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
}
