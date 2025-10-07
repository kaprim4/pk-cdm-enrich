package procheck.dev.enrich.threads;

import procheck.dev.enrich.CommonVars;
/**
 * Objet qui contient toutes les informations d'une valeur (Lot, Remise, Cheque ou LCN)
 * @author K.ABIDa
 *
 */
public class PCDocument {
	/**
	 * Type du document
	 */
	String docType;
	
	/**
	 * Type du document apres rectification 
	 */
	String docTypeRectified;
	
	/**
	 * la valeur de CMCM7
	 */
	public String CMC7;
	
	/**
	 * boolean indique si l'image recto TG existe ou non
	 */
	boolean isImgFrontColorExist;
	
	/**
	 * boolean indique si l'image verso TG existe ou non
	 */
	boolean isImgRearColorExist;
	
	/**
	 * la ligne de la valeur dans le fichier *.lot
	 */
	String lineInFile;
	
	/**
	 * offset de l'image recto noir et blanc dans le fichier des images
	 */
	int startRectoBW;
	
	/**
	 * la langueur de l'image recto noir et blanc 
	 */
	int lengthRectoBW;
	
	/**
	 * offset de l'image verso noir et blanc dans le fichier des images
	 */
	int startVersoBW;
	
	/**
	 * la langueur de l'image verso noir et blanc 
	 */
	int lengthVersoBW;
	
	/**
	 * offset de l'image recto TG dans le fichier des images
	 */
	int startRectoColor;
	
	/**
	 * la langueur de l'image recto TG
	 */
	int lengthRectoColor;
	
	/**
	 * offset de l'image verso TG dans le fichier des images
	 */
	int startVersoColor;
	
	/**
	 * la langueur de l'image verso TG
	 */
	int lengthVersoColor;
	
	/**
	 * la sequence de la valeur
	 */
	String seqDoc;

	/**
	 * l'image recto noir et blanc
	 */
	byte[] imageBWFront;
	
	/**
	 * l'image verso noir et blanc
	 */
	byte[] imageBWRear;
	
	/**
	 * l'image recto TG
	 */
	byte[] imageColorFront;
	
	/**
	 * l'image verso TG
	 */
	byte[] imageColorRear;
	
	/**
	 * contient l'erreur si le traitement retourne un probleme
	 */
	String erreurMsg;
	
	/**
	 * nom du fichier grain
	 */
	String fileName;
	
	/**
	 * la source de capture
	 */
	String source;
	
	/**
	 * cmc7 de la valeur apres rectification
	 */
	String cmc7FromRectified;
	
	/**
	 * cmc7 retourné par le serveur CMC7
	 */
	String cmc7FromOCRServer;
	
	/**
	 * cmc7 retourné par le serveur AI CMC7
	 */
	String cmc7FromAIServer;
	/**
	 * cmc7 retourné par function final
	 */
	String cmc7Final;
		
	public String getCmc7Final() {
		return cmc7Final;
	}

	public void setCmc7Final(String cmc7Final) {
		this.cmc7Final = cmc7Final;
	}

	/**
	 * Point de capture
	 */
	String capturePoint;
	
	/**
	 * Montant chiffre lu par l'ICR AI pour les cheques
	 */
	public String mntICRHisto;
	/**
	 * Montant lettre lu par l'ICR AI pour les cheques
	 */
	public String mntLettreICRHisto;
	/**
	 * Probabilté retournée par le serveur ICR
	 */
	public double probability;
	
	/**
	 * Constructeur d'objet
	 * @param lineInFile la ligne dans le fichier .lot
	 * @param fName le nom du fichier grains
	 * @param capturePointIn point de capture
	 */
	
	public PCDocument(String lineInFile, String fName,String capturePointIn) {
		isImgFrontColorExist = false;
		isImgRearColorExist = false;
		this.CMC7 = null;
		docType = "00";
		this.lineInFile = lineInFile;
		fileName = fName;
		source ="UNKNOWN";
		this.capturePoint = capturePointIn;
		mntICRHisto="";
	}

	/**
	 * Recuperation des données de la valeur à partir de ligne .lot grain de type LPJ
	 * @return true si ok, false sinon
	 */
	public boolean loadDataLotPakJPK() {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start PCDocument.loadDataLotPakJPK");
			docType = this.lineInFile.substring(0, 2);
			if (isOk) {
				this.CMC7 = this.lineInFile.substring(9, 73);
				this.isImgFrontColorExist = this.lineInFile.substring(106, 107).equals("1");
				this.isImgRearColorExist = this.lineInFile.substring(108, 109).equals("1");
				this.startRectoBW = Integer.parseInt(lineInFile.substring(265, 275)) - 1;
				this.lengthRectoBW = Integer.parseInt(lineInFile.substring(255, 264));
				this.startVersoBW = Integer.parseInt(lineInFile.substring(286, 296)) - 1;
				this.lengthVersoBW = Integer.parseInt(lineInFile.substring(276, 285));
				this.startRectoColor = Integer.parseInt(lineInFile.substring(307, 317)) - 1;
				this.lengthRectoColor = Integer.parseInt(lineInFile.substring(297, 306));
				this.startVersoColor = Integer.parseInt(lineInFile.substring(328, 338)) - 1;
				this.lengthVersoColor = Integer.parseInt(lineInFile.substring(318, 327));
				this.seqDoc = lineInFile.substring(2, 8).trim();
			}
		} catch (Exception ee) {
			CommonVars.logger.error("#PCDocument.loadDataLotPakJPK#", ee);
			erreurMsg = "Exception#PCDocument.loadDataLotPakJPK#[" + ee.getMessage() + "]";
			isOk = false;
		} finally {
			CommonVars.logger.info("End PCDocument.loadDataLotPakJPK");
		}
		return isOk;
	}

	/**
	 * recuperation des images de la valeur à partir des grains de type LPJ
	 * @param fileBWContent le fichier grains qui contient les images NB
	 * @param fileColorContent le fichier grains qui contient les images TG
	 * @return true si ok, false sinon
	 */
	public boolean loadImagesLotPakJPK(byte[] fileBWContent, byte[] fileColorContent) {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start PCDocument.loadImagesLotPakJPK");
			if (this.lengthRectoBW == 0) {
				imageBWFront = null;
			} else {
				imageBWFront = new byte[this.lengthRectoBW];
				System.arraycopy(fileBWContent, this.startRectoBW, imageBWFront, 0, this.lengthRectoBW);
			}

			if (this.lengthVersoBW == 0) {
				imageBWRear = null;
			} else {
				this.imageBWRear = new byte[this.lengthVersoBW];
				System.arraycopy(fileBWContent, this.startVersoBW, imageBWRear, 0, this.lengthVersoBW);
			}

			if (this.lengthRectoColor == 0) {
				imageColorFront = null;
			} else {
				this.imageColorFront = new byte[this.lengthRectoColor];
				System.arraycopy(fileColorContent, this.startRectoColor, imageColorFront, 0, this.lengthRectoColor);
			}
			if (this.lengthVersoColor == 0) {
				imageColorRear = null;
			} else {
				this.imageColorRear = new byte[this.lengthVersoColor];
				System.arraycopy(fileColorContent, this.startVersoColor, imageColorRear, 0, this.lengthVersoColor);
			}
		} catch (Exception ee) {
			CommonVars.logger.error("#PCDocument.loadImagesLotPakJPK#", ee);
			erreurMsg = "Exception#PCDocument.loadImagesLotPakJPK#[" + ee.getMessage() + "]";
			isOk = false;
		} finally {
			CommonVars.logger.info("End PCDocument.loadImagesLotPakJPK");
		}
		return isOk;
	}

	/**
	 * recuperation des images de la valeur à partir des grains de type Idx/Fim/Rim
	 * @param fileBWContent le fichier grains qui contient les images NB
	 * @param fileColorContent le fichier grains qui contient les images recto TG
	 * @param fileColorContentRear le fichier grains qui contient les images verso TG
	 * @return true si ok, false sinon
	 */
	public boolean loadImagesIdxFimRim(byte[] fileBWContent, byte[] fileColorContent, byte[] fileColorContentRear) {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start PCDocument.loadImagesIdxFimRim");
			if (this.lengthRectoBW == 0) {
				imageBWFront = null;
			} else {
				imageBWFront = new byte[this.lengthRectoBW];
				System.arraycopy(fileBWContent, this.startRectoBW, imageBWFront, 0, this.lengthRectoBW);
			}

			if (this.lengthRectoColor == 0) {
				imageColorFront = null;
			} else {
				this.imageColorFront = new byte[this.lengthRectoColor];
				System.arraycopy(fileColorContent, this.startRectoColor, imageColorFront, 0, this.lengthRectoColor);
			}
			if (this.lengthVersoColor == 0) {
				imageColorRear = null;
			} else {
				this.imageColorRear = new byte[this.lengthVersoColor];
				System.arraycopy(fileColorContentRear, this.startVersoColor, imageColorRear, 0, this.lengthVersoColor);
			}

		} catch (Exception ee) {
			CommonVars.logger.error("#PCDocument.loadImagesIdxFimRim#", ee);
			erreurMsg = "Exception#PCDocument.loadImagesIdxFimRim#[" + ee.getMessage() + "]";
			isOk = false;
		} finally {
			CommonVars.logger.info("End PCDocument.loadImagesIdxFimRim");
		}
		return isOk;
	}

	/**
	 * Recuperation des données de la valeur à partir de ligne .IDX grain de type Idx/Fim/Rim
	 * @return true si ok, false si non
	 */
	public boolean loadDataIdxFimRim() {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start PCDocument.loadDataIdxFimRim");
			docType = this.lineInFile.substring(0, 2);
			if (isOk) {
				this.CMC7 = this.lineInFile.substring(9, 73);
				this.isImgFrontColorExist = this.lineInFile.substring(106, 107).equals("1");
				this.isImgRearColorExist = this.lineInFile.substring(108, 109).equals("1");
				this.seqDoc = lineInFile.substring(2, 9).trim();
			}
		} catch (Exception ee) {
			CommonVars.logger.error("#PCDocument.loadDataIdxFimRim#", ee);
			erreurMsg = "Exception#PCDocument.loadDataIdxFimRim#[" + ee.getMessage() + "]";
			isOk = false;
		} finally {
			CommonVars.logger.info("End PCDocument.loadDataIdxFimRim");
		}
		return isOk;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public String getCMC7() {
		return CMC7;
	}

	public void setCMC7(String cMC7) {
		CMC7 = cMC7;
	}

	public boolean isImgFrontColorExist() {
		return isImgFrontColorExist;
	}

	public void setImgFrontColorExist(boolean isImgFrontColorExist) {
		this.isImgFrontColorExist = isImgFrontColorExist;
	}

	public boolean isImgRearColorExist() {
		return isImgRearColorExist;
	}

	public void setImgRearColorExist(boolean isImgRearColorExist) {
		this.isImgRearColorExist = isImgRearColorExist;
	}

	public String getLineInFile() {
		return lineInFile;
	}

	public void setLineInFile(String lineInFile) {
		this.lineInFile = lineInFile;
	}

	public int getStartRectoBW() {
		return startRectoBW;
	}

	public void setStartRectoBW(int startRectoBW) {
		this.startRectoBW = startRectoBW;
	}

	public int getLengthRectoBW() {
		return lengthRectoBW;
	}

	public void setLengthRectoBW(int lengthRectoBW) {
		this.lengthRectoBW = lengthRectoBW;
	}

	public int getStartVersoBW() {
		return startVersoBW;
	}

	public void setStartVersoBW(int startVersoBW) {
		this.startVersoBW = startVersoBW;
	}

	public int getLengthVersoBW() {
		return lengthVersoBW;
	}

	public void setLengthVersoBW(int lengthVersoBW) {
		this.lengthVersoBW = lengthVersoBW;
	}

	public int getStartRectoColor() {
		return startRectoColor;
	}

	public void setStartRectoColor(int startRectoColor) {
		this.startRectoColor = startRectoColor;
	}

	public int getLengthRectoColor() {
		return lengthRectoColor;
	}

	public void setLengthRectoColor(int lengthRectoColor) {
		this.lengthRectoColor = lengthRectoColor;
	}

	public int getStartVersoColor() {
		return startVersoColor;
	}

	public void setStartVersoColor(int startVersoColor) {
		this.startVersoColor = startVersoColor;
	}

	public int getLengthVersoColor() {
		return lengthVersoColor;
	}

	public void setLengthVersoColor(int lengthVersoColor) {
		this.lengthVersoColor = lengthVersoColor;
	}

	public String getSeqDoc() {
		return seqDoc;
	}

	public void setSeqDoc(String seqDoc) {
		this.seqDoc = seqDoc;
	}

	public byte[] getImageBWFront() {
		return imageBWFront;
	}

	public void setImageBWFront(byte[] imageBWFront) {
		this.imageBWFront = imageBWFront;
	}

	public byte[] getImageBWRear() {
		return imageBWRear;
	}

	public void setImageBWRear(byte[] imageBWRear) {
		this.imageBWRear = imageBWRear;
	}

	public byte[] getImageColorFront() {
		return imageColorFront;
	}

	public void setImageColorFront(byte[] imageColorFront) {
		this.imageColorFront = imageColorFront;
	}

	public byte[] getImageColorRear() {
		return imageColorRear;
	}

	public void setImageColorRear(byte[] imageColorRear) {
		this.imageColorRear = imageColorRear;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getErreurMsg() {
		return erreurMsg;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the cmc7FromRectified
	 */
	public String getCmc7FromRectified() {
		return cmc7FromRectified;
	}

	/**
	 * @param cmc7FromRectified the cmc7FromRectified to set
	 */
	public void setCmc7FromRectified(String cmc7FromRectified) {
		this.cmc7FromRectified = cmc7FromRectified;
	}

	/**
	 * @return the cmc7FromOCRServer
	 */
	public String getCmc7FromOCRServer() {
		return cmc7FromOCRServer;
	}

	/**
	 * @param cmc7FromOCRServer the cmc7FromOCRServer to set
	 */
	public void setCmc7FromOCRServer(String cmc7FromOCRServer) {
		this.cmc7FromOCRServer = cmc7FromOCRServer;
	}

	/**
	 * @param erreurMsg the erreurMsg to set
	 */
	public void setErreurMsg(String erreurMsg) {
		this.erreurMsg = erreurMsg;
	}

	/**
	 * @return the docTypeRectified
	 */
	public String getDocTypeRectified() {
		return docTypeRectified;
	}

	/**
	 * @param docTypeRectified the docTypeRectified to set
	 */
	public void setDocTypeRectified(String docTypeRectified) {
		this.docTypeRectified = docTypeRectified;
	}

	/**
	 * @return the capturePoint
	 */
	public String getCapturePoint() {
		return capturePoint;
	}

	/**
	 * @return the cmc7FromAIServer
	 */
	public String getCmc7FromAIServer() {
		return cmc7FromAIServer;
	}

	/**
	 * @param cmc7FromAIServer the cmc7FromAIServer to set
	 */
	public void setCmc7FromAIServer(String cmc7FromAIServer) {
		this.cmc7FromAIServer = cmc7FromAIServer;
	}
}
