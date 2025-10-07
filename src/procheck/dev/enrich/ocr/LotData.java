package procheck.dev.enrich.ocr;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import procheck.dev.enrich.CommonVars;
import procheck.dev.lib.PCHStringOpr;

/**
 * Objet qui contient les données lu par l'OCR pour le lot BMCE
 * @author K.ABIDA
 *
 */
public class LotData {
	
	/**
	 * la date du lot
	 */
	String dateR;
	/**
	 * la reference du lot
	 */
	String ref;
	
	/**
	 * le montant du lot
	 */
	String amount;
	
	/**
	 * l'agence remettante
	 */
	String agence;
	
	/**
	 * nombre de valeur dans le lot
	 */
	String nbValeur;

	public LotData() {

	}

	/**
	 * chargement des données lot
	 * @param data les données lu par l'OCR
	 * @param type type d'image lu (BW ou TG)
	 * @param field le champs à recuperer
	 */
	public void loadData(String data, String type, String field) {
		String dataIn[] = data.split("\n");
		ArrayList<String> arrayList = new ArrayList<String>();
		CommonVars.logger.debug("***************** " + type + "|" + field + " ********************");
		for (String string : dataIn) {
			if (string != null && string.trim().length() > 0) {
				arrayList.add(string);
				CommonVars.logger.debug(string);
			}
		}
		CommonVars.logger.debug("******************************************");
		try {
			for (String str : arrayList) {
				if (field.equalsIgnoreCase("CodeAgence")) {
					this.agence = PCHStringOpr.getDegitsFromString(str, 5, false);
					if (this.agence != null) {
						if (this.agence.startsWith("00")) {
							this.agence = null;
						}
						break;
					}
				} else if (field.equalsIgnoreCase("reference")) {
					this.ref = PCHStringOpr.getDegitsFromString(str, 9, false);
					if (this.ref != null) {
						break;
					}
				} else if (field.equalsIgnoreCase("montant")) {
					this.amount = PCHStringOpr.getDegitsFromString(str == null ? null : str.replace(" ", ""), -1, true);
					if (this.amount != null) {
						break;
					}
				} else if (field.equalsIgnoreCase("nbrremise")) {
					this.nbValeur = PCHStringOpr.getDegitsFromString(str, 2, false);
					if (this.nbValeur != null) {
						break;
					} else {
						this.nbValeur = PCHStringOpr.getDegitsFromString(str, 1, false);
						if (this.nbValeur != null) {
							break;
						}
					}
				} else if (field.equalsIgnoreCase("date")) {
					this.dateR = PCHStringOpr.getDateFromString(str);
					if (this.dateR != null && isCorrectDate(dateR, "ddMMyy")) {
						break;
					} else {
						this.dateR = null;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
/**
 * verifier si une date lot est correcte
 * @param dateToCheck la date en question
 * @param formatStr le format de la date
 * @return true si ok false si non
 */
	private boolean isCorrectDate(String dateToCheck, String formatStr) {
		boolean isOk = true;
		try {
			DateFormat format = new SimpleDateFormat(formatStr);
			format.setLenient(false);
			format.parse(dateToCheck);
		} catch (Exception e) {
			isOk = false;
		}
		return isOk;
	}

	@Override
	public String toString() {
		return "Data Lot [Date :" + dateR + "|ref :" + ref + "|Amount :" + amount + "|Agence :" + agence
				+ "|NbrRemise :" + nbValeur + "]";
	}

	public String getDateR() {
		return dateR == null ? "" : dateR;
	}

	public void setDateR(String dateR) {
		this.dateR = dateR;
	}

	public String getRef() {
		return ref == null ? "" : ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getAmount() {
		return amount == null ? "" : amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getAgence() {
		return agence == null ? "" : agence;
	}

	public void setAgence(String agence) {
		this.agence = agence;
	}

	public String getNbValeur() {
		return nbValeur == null ? "" : nbValeur;
	}

	public void setNbValeur(String nbValeur) {
		this.nbValeur = nbValeur;
	}
}