package procheck.dev.enrich.ocr;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;

import procheck.dev.enrich.CommonVars;

/**
 * Objet qui contient la configuration OCR et CM7
 * 
 * @author K.ABIDA
 *
 */
public class ServerConfig {

	/**
	 * Adresse ip du serveur CMC7
	 */
	private String ipCmc7Server;
	/**
	 * Port du serveur CMC7
	 */
	private int portCmc7Server;

	/**
	 * Objet qui contient le parametrage du ticket lot
	 */
	private TLot tLot;

	/**
	 * Objet qui contient le parametrage du ticket remise
	 */
	private TRemise tRemise;

	/**
	 * Objet qui contient le parametrage d'une valeur de type cheque
	 */
	private Cheque cheque;

	/**
	 * Objet qui contient le parametrage d'une valeur de type LCN
	 */
	private Lcn lcn;

	/**
	 * objet qui contient le parametrage du moteur OCR tesseract
	 */
	private TesseractPatam tesseractPatam;

	/**
	 * List des repertoire d'inport par type de grains (Agence, GR, GAB, capture
	 * centralis�e et Capture WEB)
	 */
	private ArrayList<ImportDirectory> dirList = new ArrayList<ImportDirectory>();

	/**
	 * Main Pour test
	 * 
	 * @param args argument input
	 */
	public static void main(String[] args) {
		try {
			XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream("c:\\out.xml")));
			ServerConfig obj = (ServerConfig) decoder.readObject();
			decoder.close();
			System.out.println(obj.getIpCmc7Server());
			System.out.println(obj.getTLot().getCmc7().getX());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Objet qui contient la configuration du moteur OCR
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class TesseractPatam {

		/**
		 * Repertoire du moteur
		 */
		String dirParam;
		/**
		 * La langue
		 */
		String lang;

		/**
		 * la liste blanche des valeur
		 */
		String whiteList;

		public String getDirParam() {
			return dirParam;
		}

		public void setDirParam(String dirParam) {
			this.dirParam = dirParam;
		}

		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}

		public String getWhiteList() {
			return whiteList;
		}

		public void setWhiteList(String whiteList) {
			this.whiteList = whiteList;
		}

	}

	/**
	 * Objet qui contient la configuration des imports
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class ImportDirectory {

		/**
		 * Repertoire de pre-import
		 */
		String dirPreImport;
		/**
		 * Repertoire d'import qui contient les grains
		 */
		String dirImport;
		/**
		 * Repertoire qui contient le temoin *.top
		 */
		String dirImportTop;

		/**
		 * type de grain // N:lot/pak/jpk, F:fim/rim ....
		 */
		String type;

		/**
		 * nombre max de thread en parallele
		 */
		int nbMaxThread;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getNbMaxThread() {
			return nbMaxThread;
		}

		public void setNbMaxThread(int nbMaxThread) {
			this.nbMaxThread = nbMaxThread;
		}

		@Override
		public String toString() {
			return "ImportDirectory [dirPreImport=" + dirPreImport + ", dirImport=" + dirImport + ", type=" + type
					+ ", nbMaxThread=" + nbMaxThread + "]";
		}

		public String getDirPreImport() {
			return dirPreImport;
		}

		public void setDirPreImport(String dirPreImport) {
			this.dirPreImport = dirPreImport;
		}

		public String getDirImport() {
			return dirImport;
		}

		public void setDirImport(String dirImport) {
			this.dirImport = dirImport;
		}

		public String getDirImportTop() {
			return dirImportTop;
		}

		public void setDirImportTop(String dirImportTop) {
			this.dirImportTop = dirImportTop;
		}

	}

	/**
	 * Objet qui contient le parametrage du ticket lot
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class TLot {

		/**
		 * Configuration Crop CMC7 lot
		 */
		private OCRCrop cmc7;

		/**
		 * Configuration Crop Agence lot
		 */
		private OCRCrop agence;

		/**
		 * Configuration Crop reference lot
		 */
		private OCRCrop reference;

		/**
		 * Configuration Crop montant lot
		 */
		private OCRCrop montant;

		/**
		 * Configuration Crop nombre de remise lot
		 */
		private OCRCrop nbremise;

		/**
		 * Configuration Crop date lot
		 */
		private OCRCrop datelot;

		public OCRCrop getCmc7() {
			return cmc7;
		}

		public void setCmc7(OCRCrop cmc7) {
			this.cmc7 = cmc7;
		}

		public OCRCrop getAgence() {
			return agence;
		}

		public void setAgence(OCRCrop agence) {
			this.agence = agence;
		}

		public OCRCrop getReference() {
			return reference;
		}

		public void setReference(OCRCrop reference) {
			this.reference = reference;
		}

		public OCRCrop getMontant() {
			return montant;
		}

		public void setMontant(OCRCrop montant) {
			this.montant = montant;
		}

		public OCRCrop getNbremise() {
			return nbremise;
		}

		public void setNbremise(OCRCrop nbremise) {
			this.nbremise = nbremise;
		}

		public OCRCrop getDatelot() {
			return datelot;
		}

		public void setDatelot(OCRCrop datelot) {
			this.datelot = datelot;
		}

		@Override
		public String toString() {
			return "TLot [cmc7=" + cmc7 + ", agence=" + agence + ", reference=" + reference + ", montant=" + montant
					+ ", nbremise=" + nbremise + ", datelot=" + datelot + "]";
		}
	}

	/**
	 * Objet qui contient le parametrage du ticket remise
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class TRemise {

		/**
		 * Configuration Crop cmc7 remise
		 */
		OCRCrop cmc7;

		/**
		 * Configuration Crop agence remise
		 */
		OCRCrop agence;

		/**
		 * Configuration Crop numero de compte remettant
		 */
		OCRCrop numCompte;

		/**
		 * Configuration Crop reference remise
		 */
		OCRCrop reference;

		/**
		 * Configuration Crop montant remise
		 */
		OCRCrop montant;

		/**
		 * Configuration Crop date remise
		 */
		OCRCrop date;

		/**
		 * Configuration Crop nombre de valeur remise
		 */
		OCRCrop nbrCheque;

		/**
		 * Configuration Crop data remise
		 */
		OCRCrop data;

		public OCRCrop getCmc7() {
			return cmc7;
		}

		public void setCmc7(OCRCrop cmc7) {
			this.cmc7 = cmc7;
		}

		public OCRCrop getAgence() {
			return agence;
		}

		public void setAgence(OCRCrop agence) {
			this.agence = agence;
		}

		public OCRCrop getNumCompte() {
			return numCompte;
		}

		public void setNumCompte(OCRCrop numCompte) {
			this.numCompte = numCompte;
		}

		public OCRCrop getReference() {
			return reference;
		}

		public void setReference(OCRCrop reference) {
			this.reference = reference;
		}

		public OCRCrop getMontant() {
			return montant;
		}

		public void setMontant(OCRCrop montant) {
			this.montant = montant;
		}

		public OCRCrop getDate() {
			return date;
		}

		public void setDate(OCRCrop date) {
			this.date = date;
		}

		public OCRCrop getNbrCheque() {
			return nbrCheque;
		}

		public void setNbrCheque(OCRCrop nbrCheque) {
			this.nbrCheque = nbrCheque;
		}

		public OCRCrop getData() {
			return data;
		}

		public void setData(OCRCrop data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return "TRemise [cmc7=" + cmc7 + ", agence=" + agence + ", numCompte=" + numCompte + ", reference="
					+ reference + ", montant=" + montant + ", date=" + date + ", nbrCheque=" + nbrCheque + ", data="
					+ data + "]";
		}
	}

	/**
	 * Objet qui contient le parametrage cheque
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class Cheque {

		/**
		 * Configuration Crop cmc7 cheque
		 */
		OCRCrop cmc7;

		/**
		 * Configuration Crop montant cheque
		 */
		OCRCrop montant;

		public OCRCrop getCmc7() {
			return cmc7;
		}

		public void setCmc7(OCRCrop cmc7) {
			this.cmc7 = cmc7;
		}

		public OCRCrop getMontant() {
			return montant;
		}

		public void setMontant(OCRCrop montant) {
			this.montant = montant;
		}

		@Override
		public String toString() {
			return "Cheque [cmc7=" + cmc7 + ", montant=" + montant + "]";
		}
	}

	/**
	 * Objet qui contient le parametrage LCN
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class Lcn {
		/**
		 * Configuration Crop cmc7 LCN
		 */
		OCRCrop cmc7;
		/**
		 * Configuration Crop Montant LCN
		 */
		OCRCrop montant;

		/**
		 * Configuration Crop date d'echeance LCN
		 */
		OCRCrop dateEcheance;

		public OCRCrop getCmc7() {
			return cmc7;
		}

		public void setCmc7(OCRCrop cmc7) {
			this.cmc7 = cmc7;
		}

		public OCRCrop getMontant() {
			return montant;
		}

		public void setMontant(OCRCrop montant) {
			this.montant = montant;
		}

		public OCRCrop getDateEcheance() {
			return dateEcheance;
		}

		public void setDateEcheance(OCRCrop dateEcheance) {
			this.dateEcheance = dateEcheance;
		}

		@Override
		public String toString() {
			return "Lcn [cmc7=" + cmc7 + ", montant=" + montant + ", dateEcheance=" + dateEcheance + "]";
		}
	}

	/**
	 * Objet qui contient le parametrage d'un crop
	 * 
	 * @author K.ABIDA
	 *
	 */
	public static class OCRCrop {

		/**
		 * position x du crop
		 */
		private double x;

		/**
		 * position y du crop
		 */
		private double y;

		/**
		 * la langueur du crop
		 */
		private double w;

		/**
		 * l'hauteur du crop
		 */
		private double h;

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

		public double getW() {
			return w;
		}

		public void setW(double w) {
			this.w = w;
		}

		public double getH() {
			return h;
		}

		public void setH(double h) {
			this.h = h;
		}

		@Override
		public String toString() {
			return "[x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
		}
	}

	public String getIpCmc7Server() {
		return ipCmc7Server;
	}

	public void setIpCmc7Server(String ipCmc7Server) {
		this.ipCmc7Server = ipCmc7Server;
	}

	public int getPortCmc7Server() {
		return portCmc7Server;
	}

	public void setPortCmc7Server(int portCmc7Server) {
		this.portCmc7Server = portCmc7Server;
	}

	public TLot getTLot() {
		return tLot;
	}

	public void setTLot(TLot tLot) {
		this.tLot = tLot;
	}

	public TRemise getTRemise() {
		return tRemise;
	}

	public void setTRemise(TRemise tRemise) {
		this.tRemise = tRemise;
	}

	public Cheque getCheque() {
		return cheque;
	}

	public void setCheque(Cheque cheque) {
		this.cheque = cheque;
	}

	public Lcn getLcn() {
		return lcn;
	}

	public void setLcn(Lcn lcn) {
		this.lcn = lcn;
	}

	public TLot gettLot() {
		return tLot;
	}

	public void settLot(TLot tLot) {
		this.tLot = tLot;
	}

	public TRemise gettRemise() {
		return tRemise;
	}

	public void settRemise(TRemise tRemise) {
		this.tRemise = tRemise;
	}

	public ArrayList<ImportDirectory> getDirList() {
		return dirList;
	}

	public void setDirList(ArrayList<ImportDirectory> dirList) {
		this.dirList = dirList;
	}

	/**
	 * trace les donn�es de parametrage
	 */
	public void logConfig() {
		CommonVars.logger.info("Config Data");
		CommonVars.logger.info("IpCmc7Server [" + this.getIpCmc7Server() + "]");
		CommonVars.logger.info("PortCmc7Server [" + this.getPortCmc7Server() + "]");
		CommonVars.logger.info("Param tLot [" + this.getTLot().toString() + "]");
		CommonVars.logger.info("Param tRemise [" + this.getTRemise().toString() + "]");
		CommonVars.logger.info("Param Cheque [" + this.getCheque().toString() + "]");
		CommonVars.logger.info("Param LCN [" + this.getLcn().toString() + "]");
		for (ImportDirectory importDirectory : dirList) {
			CommonVars.logger.info("Param importDirectory [" + importDirectory.toString() + "]");
		}
	}

	public TesseractPatam getTesseractPatam() {
		return tesseractPatam;
	}

	public void setTesseractPatam(TesseractPatam tesseractPatam) {
		this.tesseractPatam = tesseractPatam;
	}
}
