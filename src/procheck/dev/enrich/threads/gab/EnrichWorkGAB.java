package procheck.dev.enrich.threads.gab;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.google.common.primitives.Bytes;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.tools.CMC7Tools;
import procheck.dev.enrich.tools.ImageTools;
import procheck.dev.lib.PCHDateFunc;
import procheck.dev.lib.PCHStringOpr;

/**
 * Prepare les donn�es � injecter sur la V5
 * 
 * @author N.BENZAIRA
 *
 */
public class EnrichWorkGAB {
	/**
	 * le nom du fichier XML
	 */
	String xmlFileName;
	/**
	 * le nom du fichier IMG
	 */
	String imgFileName;
	/**
	 * repertoire d'import
	 */
	String impDir;
	/**
	 * le nouveau contenue du fichier .lot apres enrichissement
	 */
	ArrayList<String> dataNewFile;

	/**
	 * le nouveau fichier NB apres traitement (compression, ajout des images
	 * virtuelles ...)
	 */
	byte[] newImagesBW = {};
	/**
	 * le nouveau fichier TG apres traitement (compression, ajout des images
	 * virtuelles ...)
	 */
	byte[] newImagesColor = {};

	/**
	 * list des images dans le fichier ZIP GAB
	 */
	List<ZipEntry> listFileImage;

	/**
	 * le fichier xml GAB
	 */
	ZipEntry xmlFile;

	/**
	 * les donn�es de la remise
	 */
	RemiseData remiseData;

	/**
	 * point de capture
	 */
	String pointDeCapture;

	/**
	 * la sequence de la valeur
	 */
	int sequence;

	/**
	 * la sequence de la remise
	 */
	int sequenceRemise;

	/**
	 * offset de l'image noir et blanc
	 */
	int imageBWOffset;

	/**
	 * l'offset de l'image TG
	 */
	int imageColorOffset;

	/**
	 * Nombre de document
	 */
	int docCount;

	/**
	 * l'erreur si le traitement rencontre un probleme
	 */
	String erreurDetail = null;

	/**
	 * Constructeur d'objet
	 * 
	 * @param fileNameIn le nom du fichier zip GAB
	 * @param impDirIn   le repertoire d'import des grains GAB
	 */
	public EnrichWorkGAB(String xmlFileName, String imgFileName, String impDirIn) {
		this.xmlFileName = xmlFileName;
		this.imgFileName = imgFileName;
		impDir = impDirIn;
		dataNewFile = new ArrayList<String>();
		listFileImage = new ArrayList<ZipEntry>();
		remiseData = new RemiseData();
		sequence = 1;
		sequenceRemise = 0;
		imageBWOffset = 1;
		imageColorOffset = 1;
		docCount = 0;
	}

	/**
	 * Fonction de traitement des grains GAB
	 * 
	 * @return true si traitement ok false sinon
	 */
	public boolean doWorkForGAB() {
	    boolean isOk = true;
	    String baseName = xmlFileName;
	    CommonVars.logger.info("Start EnrichWorkGAB.doWorkForGAB");
	    InputStream xmlStream = null;
	    InputStream imgStream = null;
	    String nameImg = "";
	    ZipFile xmlZipFile = null;
	    ZipFile imgZipFile = null;

	    try {
	    	// 80021-00181-101-2024-05-20.095916.268.cop.xml.zip
	        pointDeCapture = baseName.substring(6, 11);
	        String scannerDeCapture = baseName.substring(12, 15);

	        // Open the XML zip file
	        String xmlZipFileName = impDir + xmlFileName; // Path.of(impDir, xmlFileName).toString();	        
	        xmlZipFile = new ZipFile(xmlZipFileName);
	        Enumeration<? extends ZipEntry> xmlEntries = xmlZipFile.entries();
	        while (xmlEntries.hasMoreElements()) {		        
	            ZipEntry entry = xmlEntries.nextElement();
	            if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
	                xmlFile = entry;
	                xmlStream = xmlZipFile.getInputStream(entry);
	                break;
	            }
	        }

	        // Check if XML file was found
	        if (xmlFile == null) {
	            CommonVars.logger.error("No xml file found in zip package [" + xmlZipFileName + "]");
	            erreurDetail = "No xml file found in zip package";
	            isOk = false;
	        }

	        // Open the IMG zip file
	        String imgZipFileName = impDir + imgFileName; // Path.of(impDir, imgFileName).toString();	        
	        imgZipFile = new ZipFile(imgZipFileName);
	        Enumeration<? extends ZipEntry> imgEntries = imgZipFile.entries();
	        while (imgEntries.hasMoreElements()) {
	            ZipEntry entry = imgEntries.nextElement();
	            String entryName = entry.getName();
	            if (!entry.isDirectory() && (entryName.endsWith(".B") || entryName.endsWith(".R") || entryName.endsWith(".G") || entryName.endsWith(".V"))) {
	                listFileImage.add(entry);
	                imgStream = imgZipFile.getInputStream(entry);
	                nameImg = entry.getName();
	            }
	        }

	        // Check if image files are correct
	        if (isOk) {
	            if (listFileImage.size() == 0 || listFileImage.size() % 4 != 0) {
	                CommonVars.logger.error("Images count incorrect in zip package [" + imgZipFileName + "]");
	                erreurDetail = "Images count incorrect in zip package";
	                isOk = false;
	            }
	        }

	        // Process the XML file
	        if (isOk) {
	            isOk = remiseData.loadDataRemise(xmlStream, this.listFileImage, imgZipFile); // , nameImg
	        }

	        // Convert to LPK if everything is okay
	        if (isOk) {
	            isOk = doConvertToLPK();
	        }

	        System.gc();
	    } catch (Exception e) {
	        CommonVars.logger.error("#EnrichWorkGAB.doWorkForGAB#", e);
	        erreurDetail = "Error : #EnrichWorkGAB.doWorkForGAB# [" + e.getMessage() + "]";
	        isOk = false;
	    } finally {
	        try {
	            if (xmlStream != null) xmlStream.close();
	            if (imgStream != null) imgStream.close();
	            if (xmlZipFile != null) xmlZipFile.close();
	            if (imgZipFile != null) imgZipFile.close();
	        } catch (IOException e) {
	            CommonVars.logger.error("Error closing streams or zip files", e);
	        }
	    }

	    CommonVars.logger.info("End EnrichWorkGAB.doWorkForGAB [" + isOk + "]");
	    return isOk;
	}


	public String getFileName() {
		return xmlFileName;
	}

	public String getImpDir() {
		return impDir;
	}

	public ArrayList<String> getDataNewFile() {
		return dataNewFile;
	}

	public byte[] getNewImagesBW() {
		return newImagesBW;
	}

	public byte[] getNewImagesColor() {
		return newImagesColor;
	}

	/**
	 * Creation des grains LPK pour un import GAB
	 * 
	 * @return true si ok false sinon
	 */
	private boolean doConvertToLPK() {
		boolean isOk = true;
		CommonVars.logger.info("Start EnrichWorkGAB.doConvertToLPK");
		try {
			isOk = addHeader();
			if (isOk) {
				isOk = addTLot();
				docCount++;
			}
			if (isOk) {
				isOk = addTRemise();
				docCount++;
			}
			if (isOk) {
				isOk = addCheques();
				docCount += remiseData.chequesLst.size();
			}
			if (isOk) {
				isOk = addFooter();
			}
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.doConvertToLPK#", e);
			erreurDetail = "Error : #EnrichWorkGAB.doConvertToLPK# [" + e.getMessage() + "]";
			isOk = false;
		} finally {

		}
		CommonVars.logger.info("End EnrichWorkGAB.doConvertToLPK [" + isOk + "]");
		return isOk;
	}

	/**
	 * Ajouter l'entete pour le fichier .lot
	 * 
	 * @return true si ok false sinon
	 */
	private boolean addHeader() {
		boolean isOk = true;
		try {
			StringBuffer sbHeader = new StringBuffer();
			String dateyyyyMMdd = remiseData.dateOp.substring(4, 8) + remiseData.dateOp.substring(2, 4)
					+ remiseData.dateOp.substring(0, 2);
			sbHeader.append("ATHIC ");
			sbHeader.append(dateyyyyMMdd);
			sbHeader.append(".mdb ");
			sbHeader.append(remiseData.dateOp.substring(0, 8));
			sbHeader.append(" ");
			sbHeader.append(pointDeCapture);
			sbHeader.append(" 005 ");
			sbHeader.append(PCHStringOpr.rightPad(impDir + xmlFileName, " ", 71));
			sbHeader.append("Format A V2.7        WINCOR GAB V1.0      ");
			sbHeader.append(this.remiseData.agenceCode);
			sbHeader.append(" ");
			sbHeader.append(dateyyyyMMdd);
			sbHeader.append(" NCOR  ");
			dataNewFile.add(CommonVars.insertStringToString(sbHeader.toString(), "CAPGB", 501));
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.addHeader#", e);
			erreurDetail = "Error : #EnrichWorkGAB.addHeader# [" + e.getMessage() + "]";
			isOk = false;
		}
		return isOk;
	}

	/**
	 * ajouter le pied pour le fichier .lot
	 * 
	 * @return true si ok false sinon
	 */
	private boolean addFooter() {
		boolean isOk = true;
		try {
			dataNewFile.add("04 00004 00000 00000 00000 " + this.remiseData.atmCode
					+ "                                  WINCOR-NIXDORF CCDM ");
			dataNewFile.add("88 00004 00000 00000 00000 " + this.remiseData.atmCode
					+ "                                  WINCOR-NIXDORF CCDM ");
			dataNewFile.add(
					"99 8:0 0000004                                                                                                                                                                              ");
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.addFooter#", e);
			erreurDetail = "Error : #EnrichWorkGAB.addFooter# [" + e.getMessage() + "]";
			isOk = false;
		}
		return isOk;
	}

	/**
	 * Ajouter les donn�es du ticket lot dans le fichier .lot
	 * 
	 * @return true si ok false sinon
	 */
	private boolean addTLot() {
		CommonVars.logger.info("EnrichWorkGab.addTLot START");
		boolean isOk = true;
		try {
			String dataEnrich = CommonVars.HEAD_DATA_DOC;
			dataEnrich = CommonVars.insertStringToString(dataEnrich, "01", 11);
			StringBuffer sbTLot = new StringBuffer();
			sbTLot.append("01 ");
			sbTLot.append(PCHStringOpr.leftPad(sequence + "", "0", 5));
			sequence++;
			sbTLot.append(" ");

			sbTLot.append(PCHStringOpr.leftPad(this.remiseData.refRemise, "0", 7));
			
			sbTLot.append(PCHStringOpr.rightPad(" 555550000003", " ", 58));
			sbTLot.append(PCHStringOpr.leftPad(this.remiseData.refRemise, "0", 12));
			sbTLot.append(" 00000000 0 0000000 1 1 00000 000000000000000000000    ---SIGNATURE--- 00021 002 0 0");
			sbTLot.append(PCHStringOpr.leftPad("", " ", 81));
			sbTLot.append("0 0 ");

			String dataCmc7Lot = getCmc7DataLot(sbTLot.toString().substring(9, 73), true);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dataCmc7Lot, " ", 64), 13);

			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(pointDeCapture, " ", 10),
					77);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, " 000000000", 87);
			String montant = (String.format("%.2f", (Double.parseDouble(remiseData.remAmount) / 100))).replace(".", ",");
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(montant, " ", 18), 97);

			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad("1", " ", 4), 115);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(remiseData.dateOp, " ", 14), 119);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.leftPad("1", " ", 26),133 );
			dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 159); // Zone Libre
			
			BufferedImage imageTiff = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "555550000003.tiff"));
			BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "555550000003.tiff"));
			
			
			isOk = ImageTools.putAllDataInImageLot(imageJPG, this.remiseData.agenceCode, this.remiseData.dateOp.substring(0, 2) + "/" + this.remiseData.dateOp.substring(2, 4) + "/" + this.remiseData.dateOp.substring(4, 8));
			if (isOk) {
				isOk = ImageTools.putAllDataInImageLot(imageTiff, pointDeCapture, this.remiseData.dateOp.substring(0, 2) + "/" + this.remiseData.dateOp.substring(2, 4) + "/" + this.remiseData.dateOp.substring(4, 8));
			}
			if (isOk) {
				byte[] imageBWFrontLot = ImageTools.compressJpg(imageTiff);
				byte[] imageBWRearLot = ImageTools.compressJpg(imageTiff);
				byte[] imageColorFrontLot = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				byte[] imageColorRearLot = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				isOk = (imageBWFrontLot != null && imageBWRearLot != null && imageColorFrontLot != null
						&& imageColorRearLot != null);
				if (isOk) {
					String imageBWSize = PCHStringOpr.leftPad(imageBWFrontLot.length + "", "0", 9);
					String imageColorSize = PCHStringOpr.leftPad(imageColorFrontLot.length + "", "0", 9);

					String imageBWFrontOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
					imageBWOffset += imageBWFrontLot.length;
					String imageBWRearOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
					imageBWOffset += imageBWRearLot.length;

					String imageColorFrontOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
					imageColorOffset += imageColorFrontLot.length;
					String imageColorRearOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
					imageColorOffset += imageColorRearLot.length;

					this.newImagesBW = Bytes.concat(newImagesBW, imageBWFrontLot);
					this.newImagesBW = Bytes.concat(newImagesBW, imageBWRearLot);

					this.newImagesColor = Bytes.concat(newImagesColor, imageColorFrontLot);
					this.newImagesColor = Bytes.concat(newImagesColor, imageColorRearLot);

					String dataLine = sbTLot.toString();
					CommonVars.logger.debug("Data [" + dataLine + "] imageBWSize[" + imageBWSize + ")");
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSize, 255);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWFrontOffset, 265);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSize, 276);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWRearOffset, 286);

					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSize, 297);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorFrontOffset, 307);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSize, 318);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorRearOffset, 328);
					dataLine = CommonVars.insertStringToString(dataLine, dataEnrich, 719);
					dataNewFile.add(dataLine);
				} else {
					erreurDetail = "Error : putAllDataInImageRemise";
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.addTLot#", e);
			erreurDetail = "Error : #EnrichWorkGAB.addTLot# [" + e.getMessage() + "]";
			isOk = false;
		}
		CommonVars.logger.info("EnrichWorkGab.addTLot END ["+isOk+"]");
		return isOk;
	}

	/**
	 * Ajouter les donn�es du ticket remise dans le fichier .lot
	 * 
	 * @return true si ok false sinon
	 */
	private boolean addTRemise() {
		CommonVars.logger.info("EnrichWorkGab.addTRemise START");
		boolean isOk = true;
		try {
			String dataEnrich = CommonVars.HEAD_DATA_DOC;
			dataEnrich = CommonVars.insertStringToString(dataEnrich, "02", 11);

			StringBuffer sbTRemise = new StringBuffer();
			String dateyyyyMMdd = PCHDateFunc.getyyyyMMddFromDate(new Date());
			sbTRemise.append("02 ");
			sbTRemise.append(PCHStringOpr.leftPad(sequence + "", "0", 5));
			sequenceRemise = sequence;

			sbTRemise.append(" ");
			sbTRemise.append(PCHStringOpr.leftPad(this.remiseData.refRemise, "0", 7));
			sbTRemise.append(PCHStringOpr.rightPad(" 444440000033", " ", 58));
			sbTRemise.append(PCHStringOpr.leftPad(this.remiseData.refRemise, "0", 12));
			sbTRemise.append(" ");
			sbTRemise.append(PCHStringOpr.leftPad(sequenceRemise + "", "0", 8));
			sbTRemise.append(" 0 0000000 1 1 00000 00021" + dateyyyyMMdd + "099"
					+ PCHStringOpr.leftPad(sequence + "", "0", 5) + "    ---SIGNATURE--- 00021 002 0 0");
			sbTRemise.append(PCHStringOpr.leftPad("", " ", 81));
			sbTRemise.append("0 0 ");

			String dataCm7cRemise = getCmc7DataRemise(sbTRemise.toString().substring(9, 73), true);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(dataCm7cRemise, " ", 64),
					13);
			dataEnrich = CommonVars.insertStringToString(dataEnrich,
					PCHStringOpr.rightPad(remiseData.agenceCode, " ", 10), 77);
			dataEnrich = CommonVars.insertStringToString(dataEnrich,
					PCHStringOpr.leftPad(remiseData.refRemise, "0", 9), 87);
			String montant = (String.format("%.2f", (Double.parseDouble(remiseData.remAmount) / 100))).replace(".",
					",");
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(montant, " ", 18), 97);
			dataEnrich = CommonVars.insertStringToString(dataEnrich,
					PCHStringOpr.rightPad(remiseData.chequesLst.size() + "", " ", 3), 115);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 132); // Date heure capture de la remise
																		// (ddmmyyyyhhmiss)
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(remiseData.dateOp, " ", 14),
					119);
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(remiseData.compteR, " ", 24),
					133);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 181); // Zone Libre

			BufferedImage imageTiff = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "444440000033.tiff"));
			BufferedImage imageJPG = ImageIO.read(new File(Main.IMAGE_PATH_FOR_VIRTUAL + "CHQ-1.jpg"));

			

			//isOk = ImageTools.putAllDataInImageRemise(imageJPG, remiseData.agenceCode, remiseData.compteR.substring(5),
			isOk = ImageTools.putAllDataInImageRemise(imageJPG, remiseData.agenceCode, remiseData.compteR,
					PCHStringOpr.leftPad(remiseData.refRemise, "0", 7), remiseData.chequesLst.size() + "", montant,
					this.remiseData.dateOp.substring(0, 2) + "/" + this.remiseData.dateOp.substring(2, 4) + "/"
							+ this.remiseData.dateOp.substring(4, 8),
					null, "");
			if (isOk) {
				isOk = ImageTools.putAllDataInImageRemise(
						imageTiff, remiseData.agenceCode, remiseData.compteR, remiseData.refRemise,
						remiseData.chequesLst.size() + "", montant, this.remiseData.dateOp.substring(0, 2) + "/"
								+ this.remiseData.dateOp.substring(2, 4) + "/" + this.remiseData.dateOp.substring(4, 8),
						null, "");
			}
			sequence++;
			if (isOk) {
				byte[] imageBWFront = ImageTools.compressJpg(imageTiff);
				byte[] imageBWRear = ImageTools.compressJpg(imageTiff);
				byte[] imageColorFront = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				byte[] imageColorRear = ImageTools.toByteArrayAutoClosable(imageJPG, "jpg");
				isOk = (imageBWFront != null && imageBWRear != null && imageColorFront != null
						&& imageColorRear != null);
				if (isOk) {
					String imageBWSize = PCHStringOpr.leftPad(imageBWFront.length + "", "0", 9);
					String imageColorSize = PCHStringOpr.leftPad(imageColorFront.length + "", "0", 9);

					String imageBWFrontOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
					imageBWOffset += imageBWFront.length;
					String imageBWRearOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
					imageBWOffset += imageBWRear.length;

					String imageColorFrontOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
					imageColorOffset += imageColorFront.length;
					String imageColorRearOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
					imageColorOffset += imageColorRear.length;

					this.newImagesBW = Bytes.concat(newImagesBW, imageBWFront);
					this.newImagesBW = Bytes.concat(newImagesBW, imageBWRear);

					this.newImagesColor = Bytes.concat(newImagesColor, imageColorFront);
					this.newImagesColor = Bytes.concat(newImagesColor, imageColorRear);
					String dataLine = sbTRemise.toString();
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSize, 255);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWFrontOffset, 265);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSize, 276);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWRearOffset, 286);

					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSize, 297);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorFrontOffset, 307);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSize, 318);
					dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorRearOffset, 328);
					dataLine = CommonVars.insertStringToString(dataLine, dataEnrich, 719);
					dataNewFile.add(dataLine);
				} else {
					erreurDetail = "Error : putAllDataInImageRemise";
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.addTRemise#", e);
			erreurDetail = "Error : #EnrichWorkGAB.addTRemise# [" + e.getMessage() + "]";
			isOk = false;
		}
		CommonVars.logger.info("EnrichWorkGab.addTRemise END ["+isOk+"]");
		return isOk;
	}

	/**
	 * Ajouter les donn�es de la valeur dans le fichier .lot
	 * 
	 * @return true si ok false sinon
	 */
	private boolean addCheques() {
		CommonVars.logger.info("EnrichWorkGab.addCheques START");
		boolean isOk = true;
		try {
			String dateyyyyMMdd = PCHDateFunc.getyyyyMMddFromDate(new Date());
			for (ChequeData cheque : remiseData.chequesLst) {
				String dataEnrich = CommonVars.HEAD_DATA_DOC;
				StringBuffer sbCheque = new StringBuffer();
				sbCheque.append("03 ");
				sbCheque.append(PCHStringOpr.leftPad(sequence + "", "0", 5));

				sbCheque.append(" ");
				sbCheque.append(PCHStringOpr.rightPad(cheque.getCmc7(), " ", 65));
				sbCheque.append(PCHStringOpr.leftPad(this.remiseData.refRemise, "0", 12));
				sbCheque.append(" ");
				sbCheque.append(PCHStringOpr.leftPad(sequenceRemise + "", "0", 8));
				sbCheque.append(" 0 0000000 1 1 00000 00021" + dateyyyyMMdd + "099" + PCHStringOpr.leftPad(sequence + "", "0", 5) + "    ---SIGNATURE--- 00021 002 0 0");
				sbCheque.append(PCHStringOpr.leftPad("", " ", 81));
				sbCheque.append("0 0 ");

				dataEnrich = CommonVars.insertStringToString(dataEnrich, "03", 11);

				dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(CMC7Tools.getCMC7Data(sbCheque.toString().substring(9, 73)), " ", 64), 13);

				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 96);
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 114); // Montant
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 118);
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 132); // Date Echeance si LCN
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 156); // Numero de compte beneficiaire
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 157); // Cheque endossable (N/Y)
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 158); // Compte en dirham convertible (N/Y)
				dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 181); // Zone Libre

				sequence++;
				byte[] imgBWFront = ImageTools.compressJpg(cheque.imgBWFront);
				byte[] imgBWRear = ImageTools.compressJpg(cheque.imgBWRear);
				/*if (imgBWFront == null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(cheque.imgBWFront, "tiff", baos);
					baos.flush();
					imgBWFront = baos.toByteArray();
					baos.close();
				}
				if (imgBWRear == null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(cheque.imgBWRear, "tiff", baos);
					baos.flush();
					imgBWRear = baos.toByteArray();
					baos.close();
				}*/
				String imageBWSizeFront = PCHStringOpr.leftPad(imgBWFront.length + "", "0", 9);
				String imageBWSizeRear = PCHStringOpr.leftPad(imgBWRear.length + "", "0", 9);

				byte[] imgColorFront = ImageTools.toByteArrayAutoClosable(cheque.imgColorFront, "jpg");
				byte[] imgColorRear = ImageTools.toByteArrayAutoClosable(cheque.imgColorRear, "jpg");
				String imageColorSizeFront = PCHStringOpr.leftPad(imgColorFront.length + "", "0", 9);
				String imageColorSizeRear = PCHStringOpr.leftPad(imgColorRear.length + "", "0", 9);

				String imageBWFrontOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
				imageBWOffset += imgBWFront.length;
				String imageBWRearOffset = PCHStringOpr.leftPad(imageBWOffset + "", "0", 10);
				imageBWOffset += imgBWRear.length;

				String imageColorFrontOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
				imageColorOffset += imgColorFront.length;
				String imageColorRearOffset = PCHStringOpr.leftPad(imageColorOffset + "", "0", 10);
				imageColorOffset += imgColorRear.length;

				this.newImagesBW = Bytes.concat(newImagesBW, imgBWFront);
				this.newImagesBW = Bytes.concat(newImagesBW, imgBWRear);

				this.newImagesColor = Bytes.concat(newImagesColor, imgColorFront);
				this.newImagesColor = Bytes.concat(newImagesColor, imgColorRear);

				String dataLine = sbCheque.toString();
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSizeFront, 255);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWFrontOffset, 265);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWSizeRear, 276);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageBWRearOffset, 286);

				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSizeFront, 297);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorFrontOffset, 307);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorSizeRear, 318);
				dataLine = CommonVars.insertStringToStringWithErase(dataLine, imageColorRearOffset, 328);
				dataLine = CommonVars.insertStringToString(dataLine, dataEnrich, 719);
				dataNewFile.add(dataLine);
			}
		} catch (Exception e) {
			CommonVars.logger.error("#EnrichWorkGAB.addCheques#", e);
			erreurDetail = "Error : #EnrichWorkGAB.addCheques# [" + e.getMessage() + "]";
			isOk = false;
		}
		CommonVars.logger.info("EnrichWorkGab.addCheques END ["+isOk+"]");
		return isOk;
	}

	/**
	 * recuperation des donn�es CMC7 pour le ticket lot
	 * 
	 * @param cmc7Ligne la ligne CMC7
	 * @param isVirtual specifie si virtuel
	 * @return la cmc7 rectifi�e
	 */
	private String getCmc7DataLot(String cmc7Ligne, boolean isVirtual) {
		String dataRet = "";
		String regex = "(\\d){7};55555(\\d){7}";
		try {
			if (isVirtual) {
				dataRet = cmc7Ligne.substring(0, 7) + ";" + cmc7Ligne.substring(8, 20);
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

	/**
	 * recuperation des donn�es CMC7 pour le ticket remise
	 * 
	 * @param cmc7Ligne la ligne qui contient la valeur de CMC7
	 * @param isVirtual specifie si virtuel
	 * @return la cmc7 rectifi�e
	 */
	private String getCmc7DataRemise(String cmc7Ligne, boolean isVirtual) {
		String dataRet = "";
		String regex = "(\\d){7};44444(\\d){7}";
		try {
			if (isVirtual) {
				dataRet = cmc7Ligne.substring(0, 7) + ";" + cmc7Ligne.substring(8, 20);
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
}
