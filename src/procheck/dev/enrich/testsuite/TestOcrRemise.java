package procheck.dev.enrich.testsuite;

import java.io.File;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.lib.PCHDateFunc;

public class TestOcrRemise {
	public static void main(String[] args) throws Exception {
		//File imageFile = new File("D:\\Grains\\JPK\\00014CROPREMISE_.jpg");
		File imageFile = new File("D:\\Wecheck V5\\JavaDev\\Grains\\Agence\\Import\\JPK\\00115CROPREMISE_.jpg");
		// File imageFileBW = new File("F:\\GRAINS CAPTHIC\\PAK\\00014CROPREMISE_.tif");

		ITesseract instance = new Tesseract(); // JNA Interface Mapping
		instance.setLanguage("ENG");
		instance.setDatapath("D:\\JavaWorkSpace\\tessdata");
		//instance.setPageSegMode(5);
		instance.setTessVariable("tessedit_char_whitelist", "0123456789");

		ITesseract instanceZ = new Tesseract(); // JNA Interface Mapping
		instanceZ.setLanguage("fra");
		instanceZ.setDatapath("D:\\JavaWorkSpace\\tessdata");
		instanceZ.setTessVariable("tessedit_char_whitelist", "0123456789/");

		try {
			Date dateStart = new Date();
			String result = instance.doOCR(imageFile);
			// System.out.println(result);

			// String resultBW = instanceZ.doOCR(imageFileBW);
			System.out.println("Time [" + getDiffDateSecFromNow(dateStart) + " sec]");

			RemiseData remiseData = new RemiseData(null);
			remiseData.loadData(result, "COLOR");
			System.out.println(remiseData.toString());
			String resultFr = instanceZ.doOCR(imageFile);
			RemiseData remiseDataOpt = new RemiseData(null);
			remiseDataOpt.loadData(resultFr, "FR");
			// System.out.println(resultBW);
			// RemiseData remiseDataBW = new RemiseData();
			// remiseData.loadData(resultBW,"BW");

		} catch (TesseractException e) {
			System.err.println(e.getMessage());
		}
	}

	public static int getDiffDateSecFromNow(Date dateStart) {
		Date dateEnd = new Date();
		return (int) ((Math.abs(dateEnd.getTime() - dateStart.getTime()) / 1000));
	}
}

class RemiseData {
	String dateR;
	String ref;
	String account;
	String amount;
	String name;
	String agence;
	String nbValeur;
	PCDocument docRemise;

	public RemiseData(PCDocument document) {
		docRemise = document;
	}

	public void loadData(String data, String type) {
		String dataIn[] = data.split("\n");
		ArrayList<String> arrayList = new ArrayList<String>();
		System.out.println("***************** " + type + " ********************");
		for (String string : dataIn) {
			if (string != null && string.trim().length() > 0) {
				arrayList.add(string);
				System.out.println(string);
			}
		}
		System.out.println("******************************************");
		try {

			if (dateR == null)
				dateR = getDate(arrayList.get(0));
			if (agence == null)
				agence = getAgence(arrayList.get(1));
			if (ref == null)
				ref = getRef(arrayList.get(2));
			if (account == null)
				setAccount(arrayList.get(3));
			if (nbValeur == null)
				nbValeur = getNbCheque(arrayList.get(4));

			if (amount == null) {
				amount = getAmount(arrayList.get(5));
			} else {
				String amountIn = getAmount(arrayList.get(5));
				if (arrayList.size() > 3 && arrayList.get(4).length() > 3 && amountIn == null) {
					amountIn = getAmount(arrayList.get(4));
				}
				System.out.println("amountIn [" + amountIn + "]");
				amount = amountIn != null ? (amount.equals(amountIn) ? amount : null) : amount;
			}
			if (arrayList.size() > 3 && arrayList.get(4).length() > 3 && amount == null) {
				amount = getAmount(arrayList.get(4));
			}
			System.out.println("Data [" + this.toString() + "]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String setAccount(String data) {
		String dataRet = null;
		String regex = "(\\d){2} {0,1}(\\d){4} {0,1}(\\S){1} {0,1}(\\d){9} {0,1}/ {0,1}\\w.*";
		String regex2 = "(\\d){4} {0,1}(\\S){1} {0,1}(\\d){9} {0,1}/ {0,1}\\w.*";
		String regex3 = "(\\d){2} (\\d){4} (\\S){1} (\\d){9} \\w.*";
		try {
			if (data.matches(regex)) {
				account = data.substring(0, data.indexOf("/")).replace("&", "S").replace(" ", "").replace("'", "");
				name = data.substring(data.indexOf("/") + 1).trim();
			} else if (data.matches(regex2)) {
				account = data.substring(0, data.indexOf("/")).replace("&", "S").replace(" ", "").replace("'", "");
				name = data.substring(data.indexOf("/") + 1).trim();
			} else if (data.matches(regex3)) {
				account = data.substring(0, 19).replace("&", "S").replace(" ", "").replace("'", "");
				name = data.substring(20).trim();
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	private String getAmount(String data) {
		String dataRet = null;
		String regex = "(\\d){1,9}.(\\d){0,2}";
		try {
			data = data.replace(",", ".").replace(" ", "");
			if (data.matches(regex)) {
				dataRet = data;
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	private String getNbCheque(String data) {
		String dataRet = null;
		String regex = "(\\d){1,3}";
		try {
			if (data.matches(regex)) {
				dataRet = data;
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	private String getAgence(String data) {
		String dataRet = null;
		String regex = "(\\d){4}/\\w.*";
		try {
			data = data.substring(0, 4).replace('B', '8') + data.substring(4);
			data = data.substring(0, 1).replace("D", "0") + data.substring(1);
			if (data.matches(regex)) {
				dataRet = data.substring(0, 4);
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	private String getRef(String data) {
		String dataRet = null;
		String regex = "(\\d){8}(\\D{0,}|\\d{0,})";
		try {
			if (data.matches(regex)) {
				dataRet = data.substring(0, 8);
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	private String getDate(String date) {
		String dataRet = null;
		String regex = "(\\d){8}";
		try {
			date = date.replace("/", "").replace("M", "");
			if (date.matches(regex)) {
				dataRet = date;
			}
			if (dataRet != null) {
				try {
					if (!PCHDateFunc.getDateddMMyyyy().equals(dataRet)) {
						if (!PCHDateFunc.getDateddMMyyyy().substring(4, 8).equals(dataRet.substring(4, 8))
								&& PCHDateFunc.getDateddMMyyyy().substring(0, 4).equals(dataRet.substring(0, 4))) {
							dataRet = dataRet.substring(0, 4) + PCHDateFunc.getDateddMMyyyy().substring(4, 8);
						}
					}
					if (!isCorrectDate(dataRet))
						dataRet = null;
				} catch (Exception e) {
					dataRet = null;
				}
			}
		} catch (Exception e) {
		}
		return dataRet;
	}

	public boolean insertData() {
		boolean isOk = true;
		CommonVars.logger.info("Start RemiseData.insertData");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "insert into T_SPE_OCR_REMISE_BIS "
						+ "(sysdatetime,filename,sequence,montantRemise,numCompteRemise,referenceaida,dateRemiseInterne,nbrChequeInterne) "
						+ "values (getdate(),?,?,?,?,?,?,?)";
				ps = CommonVars.ConDb.prepareStatement(query);
				ps.setString(1, this.docRemise.getFileName());
				ps.setInt(2, Integer.parseInt(this.docRemise.getSeqDoc()));
				ps.setDouble(3, Double.parseDouble(this.amount == null ? "-1" : this.amount) * 100);
				ps.setString(4, this.account);
				ps.setString(5, this.ref);
				ps.setString(6, this.dateR);
				ps.setInt(7, Integer.parseInt(this.nbValeur == null ? "-1" : this.nbValeur));
				isOk = ps.execute();
			}
		} catch (Exception e) {
			CommonVars.logger.error("#RemiseData.insertData#");
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End RemiseData.insertData[" + isOk + "]");
		}
		return isOk;
	}

	private boolean isCorrectDate(String dateToCheck) {
		boolean isOk = true;
		try {
			Date date1 = new SimpleDateFormat("ddMMyyyy").parse(dateToCheck);
			if (date1 == null) {
				isOk = false;
			}
		} catch (Exception e) {
			isOk = false;
		}
		return isOk;
	}

	@Override
	public String toString() {
		return "[" + dateR + "|" + ref + "|" + account + "|" + amount + "|" + name + "|" + agence + "|" + nbValeur
				+ "]";
	}
}
