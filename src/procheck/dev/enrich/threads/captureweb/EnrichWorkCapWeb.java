package procheck.dev.enrich.threads.captureweb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;

import org.jcodec.common.logging.Logger;

import com.google.common.primitives.Bytes;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.document.TLot;
import procheck.dev.enrich.document.TRemise;
import procheck.dev.enrich.document.Valeur;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.lib.PCHDateFunc;
import procheck.dev.lib.PCHStringOpr;

/**
 * Prepare les donn�es � injecter sur la V5
 * 
 * @author K.ABIDA
 *
 */
public class EnrichWorkCapWeb {

	/**
	 * le nom du fichier
	 */
	String fileName;

	/**
	 * repertoire d'import
	 */
	String impDir;

	/**
	 * le nouveau contenue du fichier .lot apres enrichissement
	 */
	ArrayList<String> dataNewFile;

	/**
	 * le fichier images TG
	 */
	byte[] fileColorContent;

	/**
	 * le nouveau fichier TG apres traitement (compression, ajout des images
	 * virtuelles ...)
	 */
	byte[] newImagesColor = {};

	/**
	 * le nombre de valeur
	 */
	int docCount;

	/**
	 * contient l'erreur gener� si le traiment est KO
	 */
	String erreurDetail = null;

	/**
	 * indique si un nouveau fichier image a �t� gener�
	 */
	boolean newImagesFile;

	/**
	 * la source de capture
	 */
	String source;

	/**
	 * Constructeur d'objet
	 * 
	 * @param fileNameIn le nom de fichier grain
	 * @param impDirIn   repertoire d'import
	 */
	public EnrichWorkCapWeb(String fileNameIn, String impDirIn) {
		fileName = fileNameIn;
		impDir = impDirIn;
		dataNewFile = new ArrayList<String>();
		docCount = 0;
		newImagesFile = false;
	}

	/**
	 * Traitement enrichissement
	 * 
	 * @return true si ok false sinon
	 */
	public boolean doWorkForCapWeb() {
		boolean isOk = true;
		CommonVars.logger.info("Start EnrichWorkCapWeb.doWorkForCapWeb");
		BufferedReader bReader = null, bReaderTemp = null;
		FileReader fReader = null;
		try {
			fReader = new FileReader(impDir + this.fileName + ".lot");
			bReaderTemp = new BufferedReader(fReader);
			String sCurrentLine;
			int nbrValeur = 0;
			while ((sCurrentLine = bReaderTemp.readLine()) != null) {
				if (sCurrentLine.substring(0, 3).equals("03 ") || sCurrentLine.substring(0, 3).equals("00 ")) {
					nbrValeur++;
				}
			}
			bReaderTemp.close();               
			fReader = new FileReader(impDir + this.fileName + ".lot");
			bReader = new BufferedReader(fReader);
			boolean continueOk = true;
			sCurrentLine = bReader.readLine();
			CommonVars.logger.info("La ligne :"+sCurrentLine);
			String pointDeCapture = sCurrentLine.substring(28, 33);
			String dateRemise = sCurrentLine.substring(19, 27);

			dataNewFile.add(sCurrentLine);
			String scanType = sCurrentLine.substring(500, 505);
			String valType = sCurrentLine.substring(505, 508);
			//String remiseType = sCurrentLine.substring(508, 509);
			
			CommonVars.logger.info("scanType [" + scanType + "] valType[" + valType + "] date remise +["+dateRemise+"]");
			if ( ( !scanType.equalsIgnoreCase("CAPGR")  && !scanType.equalsIgnoreCase("CAPVR") ) || !valType.equalsIgnoreCase(Main.PROJECT_NAME.equalsIgnoreCase("CHEQUE") ? "CHQ" : "LCN")) {
				
				this.erreurDetail = "Capture Web invalide " + Main.PROJECT_NAME + " | scanType [" + scanType + "] valType[" + valType + "]";
				isOk = false;
			}
			if (isOk) {
				isOk = PCHDateFunc.isValideScanDate(dateRemise, "ddMMyyyy", Main.PERIOD_ACCEPT_DATA);
				if (!isOk) {
					this.erreurDetail = "Date[" + dateRemise + "] de scan invalide !";
				}
			}			
			if (isOk) {
				isOk = loadDataImage();
				if (!isOk) {
					this.erreurDetail = "Probleme de chargement des images !";
				}
			}

			if (isOk) {

				int imageColorOffset = fileColorContent.length + 1;
				while ((sCurrentLine = bReader.readLine()) != null) {
					continueOk = true;
					String encline = sCurrentLine.substring(0, 2);
					CommonVars.logger.debug("Start processing on Document { type: "+encline+", line: "+nbrValeur+" }");
					if (encline.equals("01") || encline.equals("02") || encline.equals("03")) {
						docCount++;

						PCDocument document = new PCDocument(sCurrentLine, this.fileName, pointDeCapture);
						document.setSource(source);
						document.setDocType(encline);
						document.setDocTypeRectified(encline);
						continueOk = document.loadDataLotPakJPK();
						if (continueOk) {
							continueOk = document.loadImagesLotPakJPK(fileColorContent, fileColorContent);
						}
						erreurDetail = document.getErreurMsg();
						if (continueOk && encline.equals("01")) {
							TLot tLot = new TLot(sCurrentLine);
							
							isOk = tLot.doWorkCapWeb(document, pointDeCapture, dateRemise);
							
								if (isOk) {
										newImagesFile = true;
										String imageColorSize = PCHStringOpr.leftPad(tLot.getImageColorFront().length + "", "0", 9);
										String imageColorFrontOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
										imageColorOffset += tLot.getImageColorFront().length;
										String imageColorRearOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
										imageColorOffset += tLot.getImageColorRear().length;

										newImagesColor = Bytes.concat(newImagesColor, tLot.getImageColorFront());

										newImagesColor = Bytes.concat(newImagesColor, tLot.getImageColorRear());

										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 255);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorFrontOffset, 265);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 276);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorRearOffset, 286);

										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 297);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorFrontOffset, 307);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 318);
										sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorRearOffset, 328);
									
								} else {
									erreurDetail = tLot.getMsgError();
									continueOk = false;
								}
						Logger.info("END if 01 ["+continueOk+"] / erreur : ("+erreurDetail+")");


						} else if (continueOk && encline.equals("02")) {
							TRemise tRemise = new TRemise(sCurrentLine);
							if (tRemise.doWorkCapWeb(document, pointDeCapture, dateRemise, nbrValeur)) {

								String imageColorSize = PCHStringOpr.leftPad(tRemise.getImageColorFront().length + "", "0", 9);

								String imageColorFrontOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
								imageColorOffset += tRemise.getImageColorFront().length;
								String imageColorRearOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
								imageColorOffset += tRemise.getImageColorRear().length;
								newImagesColor = Bytes.concat(newImagesColor, tRemise.getImageColorFront());
								newImagesColor = Bytes.concat(newImagesColor, tRemise.getImageColorRear());

								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 255);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorFrontOffset, 265);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 276);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorRearOffset, 286);

								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 297);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorFrontOffset, 307);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorSize, 318);
								sCurrentLine = CommonVars.insertStringToStringWithErase(sCurrentLine, imageColorRearOffset, 328);

							} else {
								erreurDetail = "Exception TRemise.dowrkCaptureWeb";
								continueOk = false;
							}
						} else if (continueOk && encline.equals("03")) {
							if(document == null)
								CommonVars.logger.warn("Document["+encline+"] is null");
							Valeur valeur = new Valeur(sCurrentLine);
							if (valeur.doWork(document)) {
								sCurrentLine = CommonVars.insertStringToString(sCurrentLine.substring(0,719), valeur.getDataEnrich(), 719);
							} else {
								erreurDetail = "Exception Valeur.dowrk";
								continueOk = false;
							}
						} else {
							sCurrentLine = CommonVars.insertStringToString(sCurrentLine, PCHStringOpr.rightPad(CommonVars.HEAD_DATA_DOC + "00", " ", 181), 719);
						}
						CommonVars.insertDocData(document);
					}
					CommonVars.logger.debug("Stop processing on Document { type: "+encline+", line: "+nbrValeur+" }");
					dataNewFile.add(sCurrentLine);
					if (!continueOk) {
						isOk = false;
						break;
					}
					System.gc();
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkCapWeb.doWorkForCapWeb#", e);
			erreurDetail = "Exception #EnrichWorkCapWeb.doWorkForCapWeb#" + e.getMessage();
			isOk = false;
		} finally {
			try {
				fReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		CommonVars.logger.info("erreurDetail : " + erreurDetail);
		CommonVars.logger.info("End EnrichWorkCapWeb.doWorkForCapWeb [" + isOk + "]");

		return isOk;
	}

	/**
	 * chargement des images � partir des fichiers grains
	 * @return true si ok, false si non
	 */
	private boolean loadDataImage() {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start EnrichWorkCapWeb.loadDataImage");
			File fileColor = new File(impDir + this.fileName + ".jpk");
			this.fileColorContent = Files.readAllBytes(fileColor.toPath());
		} catch (Exception ee) {
			CommonVars.logger.error("#EnrichWorkCapWeb.loadDataImage#", ee);
			erreurDetail = "Exception #EnrichWorkCapWeb.loadDataImage# [" + ee.getMessage() + "]";
			isOk = false;
		} finally {
			CommonVars.logger.info("End EnrichWorkCapWeb.loadDataImage [" + isOk + "]");
		}
		return isOk;
	}

	public String getFileName() {
		return fileName;
	}

	public String getImpDir() {
		return impDir;
	}

	public ArrayList<String> getDataNewFile() {
		return dataNewFile;
	}

	public byte[] getFileColorContent() {
		return fileColorContent;
	}

	public byte[] getNewImagesColor() {
		return newImagesColor;
	}

	public boolean isNewImagesFile() {
		return newImagesFile;
	}

}
