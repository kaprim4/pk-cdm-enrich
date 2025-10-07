package procheck.dev.enrich;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import procheck.dev.enrich.dao.ConnManager;
import procheck.dev.enrich.ocr.ServerConfig;
import procheck.dev.enrich.threads.PCDocument;
/**
 * objet qui conient des fonctions et variable communnes
 * @author K.ABIDA
 *
 */
public class CommonVars {
	/**
	 * Instance de Connexion � la base de donn�es
	 */
	public static Connection ConDb;
	
	/**
	 * Ojet qui contient la configuration du module enrichissement 
	 */
	public static ServerConfig srvConf;
	
	/**
	 * logger log4j
	 */
	public final static Logger logger = Logger.getLogger("");
	
	/**
	 * le temps en secondes qu'un thread peut prendre pour reprendre un traitement
	 */
	public final static int THREAD_SLEEP_TIME = 10000;
	
	/**
	 * le temps en secondes que le thread princile peut prendre pour reprendre un traitement
	 */
	public final static int MAIN_SLEEP_TIME = 20000;
	
	/**
	 * un semaphore pour les acces concurent
	 */
	public static Semaphore mySem = new Semaphore(1);
	
	/**
	 * une pile utiliser pour stocker des informations grains
	 */
	public static HashMap<String, String> hMap = new HashMap<String, String>();

	/******************** ICR ***************************/
	
	/**
	 * L'url du serveur AI ICR
	 */
	public static String ICR_AI_URL;
	
	public static String ICR_AI_DATE;
	public static String ICR_AI_SIGN;
	
	/**
	 *  L'url du serveur AI CMC7 Valeur (chque/LCN)
	 */
	public static String OCR_AI_URL_VALEUR;
	
	/**
	 *  L'url du serveur AI CMC7 Remise
	 */
	public static String OCR_AI_URL_REMISE;
	public static String DPI;
	
	/**
	 * type du grain
	 */
	public static String TYPE_VALEUR;
	/**
	 * v�rifier si le matching entre le montant chiffre et lettre activer
	 */
	public static boolean IS_MATCHING_MONTANT_ACTIVE;
	/**
	 * l'etat de l'ICR
	 */
	public static boolean ICR_IS_ACTIVE = false;
	
	/**
	 * l'heure de debut du traitement
	 */
	public static int START_TELECOLLECTE_TIME;
	
	/**
	 * l'heure de fin du traitement
	 */
	public static int STOP_TELECOLLECTE_TIME;

	/**
	 * le header � inserer dans le fichier lot pour l'enrichissement
	 */
	public final static String HEAD_DATA_DOC = "ZNE_V5_PCH";
	public static boolean ICR_FF_IS_ACTIVE = false;

	/**
	 * ajoute une donn�e � la pile
	 * @param value la valeur � ajouter
	 */
	public static void hMapAdd(String value) {
		logger.debug("Start CommonVars.hMapAdd [" + value + "]");
		try {
			mySem.acquire();
			hMap.put(value, "");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mySem.release();
		}
		logger.debug("End CommonVars.hMapAdd");
	}

	/**
	 * supprime une valeur de la pile
	 * @param value la valeur � supprimer
	 */
	public static void hMapDelete(String value) {
		logger.debug("Start CommonVars.hMapDelete [" + value + "]");
		try {
			mySem.acquire();
			hMap.remove(value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mySem.release();
		}
		logger.debug("End CommonVars.hMapDelete");
	}

	/**
	 * test si une valeur existe dans la pile
	 * @param value la valeur � chercher
	 * @return true si exist false sinon
	 */
	public static boolean hMapExist(String value) {
		boolean isExist = false;
		logger.debug("Start CommonVars.hMapExist [" + value + "]");
		isExist = hMap.containsKey(value);
		logger.debug("End CommonVars.hMapExist[" + isExist + "]");
		return isExist;
	}

	/**
	 * se connecte � la base de donn�es
	 * @return true si connect� false sinon
	 */
	public static boolean connectToDb() {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.connectToDb");
		try {
			if (ConDb == null || ConDb.isClosed() || !ConDb.isValid(2)) {
				ConDb = ConnManager.ConnetionDB(Main.DB_URL, Main.DB_USER, Main.DB_PASSWORD, Main.DB_DRIVER);
				if (ConDb == null) {
					isOk = false;
				} else {
					ConDb.setAutoCommit(true);
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#CommonVars.connectToDb#");
			isOk = false;
		} finally {
			CommonVars.logger.info("End CommonVars.connectToDb [" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * fermer la connexion � la base de donn�es
	 */
	public static void closeDbCnx() {
		try {
			ConDb.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
	 * tester si des grains on �t� deja trait� par ce module
	 * @param fileName le nom du fichier grains
	 * @return true si deja trait� false sinon
	 */
	public static boolean isFileAlreadyExist(String fileName) {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.isFileAlreadyExist [" + fileName + "]");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "select s_original_file_name from T_HISTO_BATCH_ENRICH "
						+ " where s_original_file_name=? and n_is_processed = 1 "
						+ " and cast(dt_processed as date) = cast(getdate() as date)";
				ps = CommonVars.ConDb.prepareStatement(query);
				ps.setString(1, fileName);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					CommonVars.logger.info("File already processed !!! [" + fileName + "]");
				} else {
					isOk = false;
				}
			}
			
			
		} catch (Exception e) {
			CommonVars.logger.error("#CommonVars.isFileAlreadyExist#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.isFileAlreadyExist[" + isOk + "]");
		}
		return isOk;
	}
	


	/**
	 * inserer l'historique d'un fichier trait�
	 * @param src la source des grains (GAB, CAPWEB, capture centralis�, agence ...)
	 * @param fileName le nom du fichier source
	 * @param destFileName  le nom du fichier cible
	 * @param isProcessed resultat du traitement 1:Ok, 0:NOK
	 * @param timeScore le temps de traitement en secondes
	 * @param nbDoc nombre de valeurs trait�
	 * @param erreur l'erreur de traitement si result!=1
	 * @return true si insertion ok false sinon
	 */
	public static boolean insertFileNameDB(String src, String fileName, String destFileName, int isProcessed,
			int timeScore, int nbDoc, String erreur) {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.insertFileNameDB [" + fileName + "]");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "insert into T_HISTO_BATCH_ENRICH "
						+ "(s_source,s_original_file_name,s_destination_file_name,"
						+ "n_is_processed,dt_processed,n_time_score,n_docs_count,s_error_detail,s_project) "
						+ "values (?,?,?,?,getdate(),?,?,?,?)";
				CommonVars.logger.debug("Query : " + query);
				ps = CommonVars.ConDb.prepareStatement(query);
				ps.setString(1, src);
				ps.setString(2, fileName);
				ps.setString(3, destFileName);
				ps.setInt(4, isProcessed);
				ps.setInt(5, timeScore);
				ps.setInt(6, nbDoc);
				ps.setString(7, erreur);
				ps.setString(8, Main.PROJECT_NAME);
				ps.execute();
			}
		} catch (Exception e) {
			CommonVars.logger.error("#CommonVars.insertFileNameDB#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.insertFileNameDB[" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * insert dans la base les donn�es d'une valeur donn�e
	 * @param document un objet qui contient toutes les informations valeur
	 * @return true si insertion ok false sinon
	 */
	public static boolean insertDocData(PCDocument document) {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.insertDocData [" + document.getDocType() + "]");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "insert into T_HISTO_BATCH_ENRICH_DOC_DATA "
						+ "(s_encline,s_encline_rectified,s_cmc7_scanner,s_cmc7_rectified,s_cmc7_from_server"
						+ ",d_date,s_project,s_point_de_capture,s_icr_mnt_valeur,s_icr_mnt_lettre_valeur,s_cmc7_from_ai_server,s_icr_probability,s_cmc7_final) "
						+ "values (?,?,?,?,?,getdate(),?,?,?,?,?,?,?)";
				CommonVars.logger.debug("Query : " + query);
				ps = CommonVars.ConDb.prepareStatement(query);
				ps.setString(1, document.getDocType());
				ps.setString(2, document.getDocTypeRectified());
				ps.setString(3, document.getCMC7());
				ps.setString(4, document.getCmc7FromRectified());
				ps.setString(5, document.getCmc7FromOCRServer());
				ps.setString(6, Main.PROJECT_NAME);
				ps.setString(7, document.getCapturePoint());
				ps.setString(8, document.mntICRHisto);
				ps.setString(9, document.mntLettreICRHisto);
				ps.setString(10, document.getCmc7FromAIServer());
				ps.setString(11, document.probability+"");
				ps.setString(12, document.getCmc7Final());
				
				CommonVars.logger.info("===> T_HISTO_BATCH_ENRICH_DOC_DATA <===");
				CommonVars.logger.info("getDocType : "+document.getDocType());
				CommonVars.logger.info("getDocTypeRectified : "+ document.getDocTypeRectified());
				CommonVars.logger.info("getCMC7 : "+document.getCMC7());
				CommonVars.logger.info("getCmc7FromRectified : "+document.getCmc7FromRectified());
				CommonVars.logger.info("getCmc7FromOCRServer : "+ document.getCmc7FromOCRServer());
				CommonVars.logger.info("PROJECT_NAME : "+Main.PROJECT_NAME);
				CommonVars.logger.info("getCapturePoint : "+ document.getCapturePoint());
				CommonVars.logger.info("mntICRHisto : "+document.mntICRHisto);
				CommonVars.logger.info("mntLettreICRHisto : "+document.mntLettreICRHisto);
				CommonVars.logger.info("getCmc7FromAIServer : "+document.getCmc7FromAIServer());
				CommonVars.logger.info("probability : "+document.probability);
				CommonVars.logger.info("getCmc7Final : "+document.getCmc7Final());
				CommonVars.logger.info("______________________________________________");
				isOk = ps.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
			CommonVars.logger.error("#CommonVars.insertDocData#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.insertDocData[" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * verifier sur le controleur si le traitement peut continuer ou non
	 * @return si on peut continuer le traitement false sinon
	 */
	public static boolean checkIfCanContinue() {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.checkIfCanContinue [" + Main.STORED_PROCEDURE_CONTROLE + "]");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "exec " + Main.STORED_PROCEDURE_CONTROLE;
				ps = CommonVars.ConDb.prepareStatement(query);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					String feedback = rs.getString("feedback");
					CommonVars.logger.info("feedback [" + feedback + "]");
					isOk = feedback.equalsIgnoreCase("OK");
				} else {
					CommonVars.logger.info("No Data Returned !!! [" + Main.STORED_PROCEDURE_CONTROLE + "]");
					isOk = false;
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#CommonVars.checkIfCanContinue#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.checkIfCanContinue[" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * verifier si un lot a �t� deja trait� dans la meme journ�e
	 * @param pointDeCapture le code de point de capture
	 * @param cmc7TLot le cmc7 de lot en question
	 * @return true si le lot est deja trait� false sinon
	 */
	public static boolean isTLotAlreadyImported(String pointDeCapture, String cmc7TLot,String dateCapture) {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.isTLotAlreadyImported [" + pointDeCapture + " | " + cmc7TLot + "] ");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "insert into T_HISTO_BATCH_ENRICH_LOT_DATA (s_project,s_cmc7_scanner,d_date,s_point_de_capture) "
						+ "values" + " (?,?,convert(date,?,103),?)";
				ps = CommonVars.ConDb.prepareStatement(query);
				ps.setString(1, Main.PROJECT_NAME);
				ps.setString(2, cmc7TLot);
				ps.setString(3, dateCapture.substring(0, 2) + "/" + dateCapture.substring(2, 4) + "/" + dateCapture.substring(4, 8));
				ps.setString(4, pointDeCapture);
				ps.executeUpdate();
				isOk = false;
			}
		} catch (Exception e) {
			CommonVars.logger
					.info("Ticket lot already processed or probleme!!! [" + Main.PROJECT_NAME+" | "+ cmc7TLot + " | " + dateCapture.substring(0, 2) + "/" + dateCapture.substring(2, 4) + "/" + dateCapture.substring(4, 8) 
					+" | "+pointDeCapture+ "]");
			CommonVars.logger.error("#CommonVars.isTLotAlreadyImported#", e);
			isOk = true;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.isTLotAlreadyImported[" + isOk + "]");
		}
		return isOk;
	}


	/**
	 * insert une chaine de caracteres dans une autre en specifiant l'emplacement
	 * @param originalString la chaine en question 
	 * @param stringToBeInserted la chaine � inserer
	 * @param index l'emplacement de l'insertion
	 * @return la chaine resultante
	 */
	public static String insertStringToString(String originalString, String stringToBeInserted, int index) {
		if (originalString.length() < index - 1) {
			originalString = StringUtils.rightPad(originalString, index, ' ');
		}
		StringBuffer newString = new StringBuffer(originalString);
		newString.insert(index - 1, stringToBeInserted);
		return newString.toString();
	}

	/**
	 * insert une chaine de caracteres dans une autre en specifiant l'emplacement et en ecrasant le reste 
	 * @param originalString la chaine en question 
	 * @param stringToBeInserted la chaine � inserer
	 * @param index l'emplacement de l'insertion
	 * @return la chaine resultante
	 */
	public static String insertStringToStringWithErase(String originalString, String stringToBeInserted, int index) {
		boolean isSimpleInsert = false;
		StringBuffer newString = null;
		if (originalString.length() < index) {
			originalString = StringUtils.rightPad(originalString, index, ' ');
			isSimpleInsert = true;

		} else if (originalString.length() == index) {
			isSimpleInsert = true;
		}
		if (isSimpleInsert) {
			newString = new StringBuffer(originalString);
			newString.insert(index, stringToBeInserted);
		} else {

			newString = new StringBuffer(originalString.substring(0, index));
			newString.append(stringToBeInserted);
			newString.append(originalString.substring(index + stringToBeInserted.length()));
		}
		return newString.toString();
	}
	
	public static boolean checkIfICRActive() {
		boolean isOk = true;
		CommonVars.logger.info("Start CommonVars.checkIfICRActive ");
		PreparedStatement ps = null;
		try {
			isOk = CommonVars.connectToDb();
			if (isOk) {
				String query = "select n_is_active from T_CONFIG_ICR_OCR where  s_type='ICR' and s_libelle='MONTANT' and s_project='"+TYPE_VALEUR+"' " ;
				ps = CommonVars.ConDb.prepareStatement(query);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					int feedback = rs.getInt("n_is_active");
					CommonVars.logger.info("n_is_active [" + feedback + "]");
					isOk = feedback==1;
				} else {
					CommonVars.logger.info("No Data Returned !!!");
					isOk = false;
				}
			}
		} catch (Exception e) {
			CommonVars.logger.error("#CommonVars.checkIfICRActive#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CommonVars.logger.info("End CommonVars.checkIfICRActive[" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * deplacer un fichier
	 * @param file le fichier en question
	 * @param pathDest le path destinataire
	 * @param newFile le nouveau fichier 
	 * @param logError permit de specifier si le probleme de deplacement presente une erreur ou non
	 * @return true si le fichier est deplac� false sinon non
	 */
	public static boolean moveFile(File file, String pathDest, File newFile, boolean logError) {
		boolean isOk = true;
		try {
			logger.info("Start moveFile[" + file.getName() + "]");
			logger.debug("File [" + file.getAbsolutePath() + "] pathDest [" + pathDest + "] newFile[" + newFile + "]");
			File destFile = new File(pathDest + "/" + file.getName());
			if (newFile != null) {
				destFile = new File(pathDest + "/" + newFile.getName());
			}
			if (destFile.exists()) {
				logger.info("File already exist in dest");
				logger.info("deleting file ...");
				if (file.delete()) {
					logger.info("file deleted");
				} else {
					logger.info("can't delete file [" + file.getName() + "]");
				}
			} else {
				if (file.renameTo(destFile)) {
					logger.info("Move File After process to {" + pathDest + "} [" + file.getAbsolutePath() + "]");
				} else {
					if (logError) {
						logger.error("can't move file [" + file.getName() + "]");
					} else {
						logger.info("can't move file [" + file.getName() + "]");
					}
				}
			}
		} catch (Exception e) {
			logger.error("PCException[doWorkFoFile.moveFile]", e);
			isOk = false;
		} finally {
			logger.info("End doWorkFoFile.moveFile[" + isOk + "]");
		}
		return isOk;
	}

	/**
	 * verifier si un fichier est pr�t � etre traiter
	 * @param file le fichier en question
	 * @return true si oui false sinon
	 */
	public static boolean fileIsReadyToUse(File file) {
		boolean isOk = true;
		try {
			if (!file.renameTo(file)) {
				isOk = false;
			}
		} catch (Exception e) {
			isOk = false;
		}
		return isOk;
	}
}
