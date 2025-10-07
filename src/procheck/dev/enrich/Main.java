package procheck.dev.enrich;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import procheck.dev.enrich.ocr.ServerConfig;
import procheck.dev.enrich.threads.captureweb.ThreadCapWeb;
import procheck.dev.enrich.threads.gab.ThreadGAB;
import procheck.dev.lib.PCHDateFunc;

/**
 * Le thread Master du module
 * 
 * @author khalil
 *
 */
public class Main extends CommonVars {

	/**
	 * permit de donner un ordre au module de s'arreter
	 */
	public static boolean STOP_ME = false;

	/**
	 * permi de verifier si on peut traiter les grains au non
	 */
	public static boolean IMPORT_TELECOLLECTE_OK = false;

	/**
	 * le port du serveur, permit d'eviter de lancer le meme module plusieurs fois
	 */
	public static String PORT_SERVICE;

	/**
	 * le nom du projet
	 */
	public static String PROJECT_NAME;

	/**
	 * le code de la banque
	 */
	public static String CLIENT_CODE;

	/**
	 * socket utiliser pour eviter le lancement du meme module plusieurs fois
	 */
	public static ServerSocket serverSocket;

	/**
	 * data source de la base de donn�es
	 */
	public static String DB_URL;

	/**
	 * utilisateur base de donn�es
	 */
	public static String DB_USER;

	/**
	 * mot de passe base de donn�es
	 */
	public static String DB_PASSWORD;

	/**
	 * le pilote de la base de donn�es
	 */
	public static String DB_DRIVER;

	/**
	 * le path de images virtuelle utilis� pour cree des lot ou remise numeriquement
	 */
	public static String IMAGE_PATH_FOR_VIRTUAL;

	/**
	 * configuration des donn�es � mettre dans la remise virtuelle
	 */
	public static String REMISE_CONFIG_FOR_VIRTUAL;

	/**
	 * configuration des donn�es � mettre dans le lot virtuelle
	 */
	public static String LOT_CONFIG_FOR_VIRTUAL;

	/**
	 * specifier si on demarre avec l'OCR ou non
	 */
	public static boolean START_WITH_OCR;

	/**
	 * la periode acceptable des grains
	 */
	public static int PERIOD_ACCEPT_DATA;

	/**
	 * la procedure stock�e pour controle le module
	 */
	public static String STORED_PROCEDURE_CONTROLE;

	/**
	 * Demarrage du module
	 * 
	 * @param args arguments input
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Please put params [1 : Project Name]");
			return;
		}
		PROJECT_NAME =args[0];
		PropertyConfigurator.configure("Config\\" + PROJECT_NAME + "_log4j.properties");
		logger.info("Start Proc PKCDMEnrich.Main ["+PROJECT_NAME+"]");
		try {
			boolean continueOk = true;
			continueOk = setParams();
			if (continueOk) {
				logger.info("Start With OCR : [" + START_WITH_OCR + "]");

				continueOk = !isAppUp();
			}
			if (continueOk) {
				continueOk = loadConfig();
			}
			if (continueOk) {
				srvConf.logConfig();
				for (ServerConfig.ImportDirectory dir : srvConf.getDirList()) {
					logger.info("Starting Threads for type ["+dir.getType()+"]");
					new File(dir.getDirPreImport() + "REJECT").mkdirs();
					new File(dir.getDirPreImport() + "TRAITE").mkdirs();
					if (dir.getType().equals("W")) {
						(new ThreadCapWeb(dir)).start();
					}else if (dir.getType().equals("G")) {
						(new ThreadGAB(dir)).start();
					}
					
					else {
						logger.info("Data type not supported [" + dir.toString() + "]");
					}
				}
				
				

				
			}
			while (true && continueOk) {
				int time = Integer.parseInt(PCHDateFunc.getDateTimeHHmmssS().substring(0, 4));
				if (time < START_TELECOLLECTE_TIME || time > STOP_TELECOLLECTE_TIME) {
					logger.info("IMPORT_TELECOLLECTE_OK = false :(");
					IMPORT_TELECOLLECTE_OK = false;
				} else {
					logger.info("IMPORT_TELECOLLECTE_OK = true :)");
					IMPORT_TELECOLLECTE_OK = true;
				}
				Thread.sleep(30000);
				updateMonitoring();
				System.gc();
			}
		} catch (Exception e) {
			logger.error("#Main#", e);
		} finally {
			CommonVars.closeDbCnx();
			try {
				serverSocket.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
			logger.info("End Proc PKCDMEnrich.Main");
		}
	}

	/**
	 * recuperation de tous les parametres
	 * 
	 * @return true si ok false si non ok
	 */
	public static boolean setParams() {
		boolean isOk = true;
		logger.info("Start Main.setParams");
		try (InputStream input = new FileInputStream("Config\\" + PROJECT_NAME + "_main_config.properties")) {
			Properties prop = new Properties();
			prop.load(input);
			PORT_SERVICE = prop.getProperty("PORT_SERVICE");
			if (PORT_SERVICE == null || PORT_SERVICE.trim().length() < 1) {
				throw new Exception("PARAM [PORT_SERVICE] is not defined or difinition error !!!");
			}
			CLIENT_CODE = prop.getProperty("CLIENT_CODE");
			if (CLIENT_CODE == null || CLIENT_CODE.trim().length() < 1) {
				throw new Exception("PARAM [CLIENT_CODE] is not defined or difinition error !!!");
			}
			DB_DRIVER = prop.getProperty("DB_DRIVER");
			if (DB_DRIVER == null || DB_DRIVER.trim().length() < 1) {
				throw new Exception("PARAM [DB_DRIVER] is not defined or difinition error !!!");
			}
			DB_URL = prop.getProperty("DB_URL");
			if (DB_URL == null || DB_URL.trim().length() < 1) {
				throw new Exception("PARAM [DB_URL] is not defined or difinition error !!!");
			}
			DB_USER = prop.getProperty("DB_USER");
			if (DB_USER == null || DB_USER.trim().length() < 1) {
				throw new Exception("PARAM [DB_USER] is not defined or difinition error !!!");
			}
			DB_PASSWORD = prop.getProperty("DB_PASSWORD");
			if (DB_PASSWORD == null || DB_PASSWORD.trim().length() < 1) {
				throw new Exception("PARAM [DB_PASSWORD] is not defined or difinition error !!!");
			}
			IMAGE_PATH_FOR_VIRTUAL = prop.getProperty("IMAGE_PATH_FOR_VIRTUAL");
			if (IMAGE_PATH_FOR_VIRTUAL == null || IMAGE_PATH_FOR_VIRTUAL.trim().length() < 1) {
				throw new Exception("PARAM [IMAGE_PATH_FOR_VIRTUAL] is not defined or difinition error !!!");
			}
			REMISE_CONFIG_FOR_VIRTUAL = prop.getProperty("REMISE_CONFIG_FOR_VIRTUAL");
			if (REMISE_CONFIG_FOR_VIRTUAL == null || REMISE_CONFIG_FOR_VIRTUAL.trim().length() < 1) {
				throw new Exception("PARAM [REMISE_IMAGE_PATH_FOR_VIRTUAL] is not defined or difinition error !!!");
			} else if (REMISE_CONFIG_FOR_VIRTUAL.split(";").length != 8
					|| REMISE_CONFIG_FOR_VIRTUAL.split(",").length != 9) {
				throw new Exception("CONFIG PROBLEM FOR PARAM [REMISE_IMAGE_PATH_FOR_VIRTUAL] !!!");
			}
			LOT_CONFIG_FOR_VIRTUAL = prop.getProperty("LOT_CONFIG_FOR_VIRTUAL");
			if (LOT_CONFIG_FOR_VIRTUAL == null || LOT_CONFIG_FOR_VIRTUAL.trim().length() < 1) {
				throw new Exception("PARAM [LOT_IMAGE_PATH_FOR_VIRTUAL] is not defined or difinition error !!!");
			} else if (LOT_CONFIG_FOR_VIRTUAL.split(";").length != 2 || LOT_CONFIG_FOR_VIRTUAL.split(",").length != 3) {
				throw new Exception("CONFIG PROBLEM FOR PARAM [LOT_CONFIG_FOR_VIRTUAL] !!!");
			}
			String START_WITH_OCR_STR = prop.getProperty("START_WITH_OCR");
			if (START_WITH_OCR_STR == null) {
				throw new Exception("PARAM [START_WITH_OCR] is not defined or difinition error !!!");
			} else if (START_WITH_OCR_STR.equalsIgnoreCase("TRUE") || START_WITH_OCR_STR.equalsIgnoreCase("FALSE")) {
				START_WITH_OCR = START_WITH_OCR_STR.equalsIgnoreCase("TRUE");
			} else {
				throw new Exception("CONFIG PROBLEM FOR PARAM accept only true or false [START_WITH_OCR] !!!");
			}
			String periodAcceptData = prop.getProperty("PERIOD_ACCEPT_DATA");
			if (periodAcceptData == null || periodAcceptData.trim().length() < 1) {
				throw new Exception("PARAM [PERIOD_ACCEPT_DATA] is not defined or difinition error !!!");
			} else {
				try {
					PERIOD_ACCEPT_DATA = Integer.parseInt(periodAcceptData);
				} catch (Exception e) {
					throw new Exception("PARAM [PERIOD_ACCEPT_DATA] is not numeric !!!");
				}
			}
			STORED_PROCEDURE_CONTROLE = prop.getProperty("STORED_PROCEDURE_CONTROLE");
			if (STORED_PROCEDURE_CONTROLE == null || STORED_PROCEDURE_CONTROLE.trim().length() < 1) {
				throw new Exception("PARAM [STORED_PROCEDURE_CONTROLE] is not defined or difinition error !!!");
			}
			String ICR_IS_ACTIVE_STR = prop.getProperty("ICR_IS_ACTIVE");
			if (ICR_IS_ACTIVE_STR == null) {
				throw new Exception("PARAM [ICR_IS_ACTIVE] is not defined or difinition error !!!");
			} else if (ICR_IS_ACTIVE_STR.equalsIgnoreCase("TRUE") || ICR_IS_ACTIVE_STR.equalsIgnoreCase("FALSE")) {
				ICR_IS_ACTIVE = ICR_IS_ACTIVE_STR.equalsIgnoreCase("TRUE");
			} else {
				throw new Exception("CONFIG PROBLEM FOR PARAM accept only true or false [ICR_IS_ACTIVE] !!!");
			}
			String IS_MATCHING_MONTANT_ACTIVE_STR = prop.getProperty("IS_MATCHING_MONTANT_ACTIVE");
			if (IS_MATCHING_MONTANT_ACTIVE_STR == null) {
				throw new Exception("PARAM [IS_MATCHING_MONTANT_ACTIVE] is not defined or difinition error !!!");
			} else if (IS_MATCHING_MONTANT_ACTIVE_STR.equalsIgnoreCase("TRUE") || IS_MATCHING_MONTANT_ACTIVE_STR.equalsIgnoreCase("FALSE")) {
				IS_MATCHING_MONTANT_ACTIVE = IS_MATCHING_MONTANT_ACTIVE_STR.equalsIgnoreCase("TRUE");
			} else {
				throw new Exception("CONFIG PROBLEM FOR PARAM accept only true or false [CHECK_MATCHING_MONTANT_IS_ACTIVE] !!!");
			}
			ICR_AI_URL = prop.getProperty("ICR_AI_URL");
			if (ICR_AI_URL == null || ICR_AI_URL.trim().length() < 1) {
				throw new Exception("PARAM [ICR_AI_URL] is not defined or difinition error !!!");
			}
			OCR_AI_URL_VALEUR = prop.getProperty("OCR_AI_URL_VALEUR");
			if (OCR_AI_URL_VALEUR == null || OCR_AI_URL_VALEUR.trim().length() < 1) {
				throw new Exception("PARAM [OCR_AI_URL_VALEUR] is not defined or difinition error !!!");
			}
			
			ICR_AI_DATE = prop.getProperty("ICR_AI_DATE");
			if (ICR_AI_DATE == null || ICR_AI_DATE.trim().length() < 1) {
				throw new Exception("PARAM [ICR_AI_DATE] is not defined or difinition error !!!");
			}
			
			ICR_AI_SIGN = prop.getProperty("ICR_AI_SIGN");
			if (ICR_AI_SIGN == null || ICR_AI_SIGN.trim().length() < 1) {
				throw new Exception("PARAM [ICR_AI_SIGN] is not defined or difinition error !!!");
			}
			
			OCR_AI_URL_REMISE = prop.getProperty("OCR_AI_URL_REMISE");
			if (OCR_AI_URL_REMISE == null || OCR_AI_URL_REMISE.trim().length() < 1) {
				throw new Exception("PARAM [OCR_AI_URL_REMISE] is not defined or difinition error !!!");
			}

			TYPE_VALEUR = prop.getProperty("TYPE_VALEUR");
			if (TYPE_VALEUR == null || TYPE_VALEUR.trim().length() != 3) {
				throw new Exception("PARAM [TYPE_VALEUR] is not defined or difinition error !!!");
			}

			DPI = prop.getProperty("DPI");
			if (DPI == null || DPI.trim().length() != 3) {
				throw new Exception("PARAM [DPI] is not defined or difinition error !!!");
			}
			
			ICR_FF_IS_ACTIVE = prop.getProperty("ICR_FF_IS_ACTIVE").equalsIgnoreCase("TRUE");
			String ICR_FF_IS_ACTIVE_STR = prop.getProperty("ICR_FF_IS_ACTIVE");
			if (ICR_FF_IS_ACTIVE_STR == null) {
				throw new Exception("PARAM [ICR_FF_IS_ACTIVE] is not defined or difinition error !!!");
			} else if (ICR_FF_IS_ACTIVE_STR.equalsIgnoreCase("TRUE") || ICR_FF_IS_ACTIVE_STR.equalsIgnoreCase("FALSE")) {
				ICR_FF_IS_ACTIVE = ICR_FF_IS_ACTIVE_STR.equalsIgnoreCase("TRUE");
			} else {
				throw new Exception("CONFIG PROBLEM FOR PARAM accept only true or false [ICR_FF_IS_ACTIVE] !!!");
			}
			
			String startTelecolecteTime = prop.getProperty("START_TELECOLLECTE_TIME");
			if (startTelecolecteTime == null || startTelecolecteTime.trim().length() < 1) {
				throw new Exception("PARAM [START_TELECOLLECTE_TIME] is not defined or difinition error !!!");
			} else {
				try {
					START_TELECOLLECTE_TIME = Integer.parseInt(startTelecolecteTime);
				} catch (Exception e) {
					throw new Exception("PARAM [START_TELECOLLECTE_TIME] is not numeric !!!");
				}
			}
			String endTelecolecteTime = prop.getProperty("STOP_TELECOLLECTE_TIME");
			if (endTelecolecteTime == null || endTelecolecteTime.trim().length() < 1) {
				throw new Exception("PARAM [STOP_TELECOLLECTE_TIME] is not defined or difinition error !!!");
			} else {
				try {
					STOP_TELECOLLECTE_TIME = Integer.parseInt(endTelecolecteTime);
				} catch (Exception e) {
					throw new Exception("PARAM [STOP_TELECOLLECTE_TIME] is not numeric !!!");
				}
			}
		} catch (Exception ex) {
			isOk = false;
			logger.error("PCException[Main.setParams]", ex);
		}
		logger.info("End Main.setParams[" + isOk + "]");
		return isOk;
	}
	
	
	/**
	 * chargement de la configuration generale du module
	 * 
	 * @return true si ok false si non ok
	 */
	private static boolean loadConfig() {
		boolean isOk = true;
		logger.info("Start Main.loadConfig");
		try {
			logger.info("Load server config");
			XMLDecoder decoder = new XMLDecoder(
					new BufferedInputStream(new FileInputStream("Config\\" + PROJECT_NAME + "_service_config.xml")));
			srvConf = (ServerConfig) decoder.readObject();
			decoder.close();
		} catch (Exception ee) {
			logger.error("#Main.loadConfig#", ee);
			isOk = false;
		} finally {

		}
		logger.info("End Main.loadConfig[" + isOk + "]");
		return isOk;
	}

	/**
	 * verifier si une instance du module est deja UP ou non.
	 * 
	 * @return true si ok false si non ok
	 */
	public static boolean isAppUp() {
		boolean isOk = false;
		try {
			serverSocket = new ServerSocket(Integer.parseInt(PORT_SERVICE));
		} catch (Exception e) {
			logger.error("An app is already UP !!!");
			isOk = true;
		}
		return isOk;
	}

	/**
	 * modifier l'etat du module sur la base de donn�es, utiliser pour les alertes
	 * sur PKSupervisor
	 * 
	 * @return true si insertion ok false si non ok
	 */
	public static boolean updateMonitoring() {
		boolean isOk = true;
		logger.info("Start Main.updateMonitoring [" + PROJECT_NAME + "]");
		PreparedStatement ps = null;
		try {
			isOk = connectToDb();
			if (isOk) {
				String query = "UPDATE T_MONITOR_SERVICES set d_last_date_time=getdate() where s_project = ? and s_sevice = ? ";
				logger.debug("Query : " + query);
				ps = ConDb.prepareStatement(query);
				ps.setString(1, PROJECT_NAME);
				ps.setString(2, PROJECT_NAME.equalsIgnoreCase("CHEQUE") ? "ENRICH_CHEQUE" : "ENRICH_LCN");
				ps.execute();
			}
		} catch (Exception e) {
			logger.error("#Main.updateMonitoring#", e);
			isOk = false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info("End Main.updateMonitoring[" + isOk + "]");
		}
		return isOk;
	}
}
