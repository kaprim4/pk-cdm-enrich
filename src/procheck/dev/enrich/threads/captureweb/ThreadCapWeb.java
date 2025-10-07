package procheck.dev.enrich.threads.captureweb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.ocr.ServerConfig;
import procheck.dev.lib.PCHDateFunc;

/**
 * Thread Master qui traite les grains de type capture web
 * 
 * @author K.ABIDA
 *
 */
public class ThreadCapWeb extends Thread {
	/**
	 * Objet qui contient la configuration de l'import
	 */
	private ServerConfig.ImportDirectory impDir;

	/**
	 * un semaphore pour limter le nombre de traitement un parallele des grains
	 */
	public static Semaphore mySem;

	/**
	 * Constrcuteur d'jobet
	 * 
	 * @param impDirIn parametrage de l'import
	 */
	public ThreadCapWeb(ServerConfig.ImportDirectory impDirIn) {
		impDir = impDirIn;
		mySem = new Semaphore(impDir.getNbMaxThread(), true);
	}

	/**
	 * demarrage du thread
	 */
	public void run() {
		CommonVars.logger.info("Start ThreadCapWeb.run");
		try {
			while (true && !Main.STOP_ME) {
				CommonVars.logger.debug("CapWeb : try again...");
				String[] extensions = new String[] { "top", "TOP", "Top" };
				File importFolder = new File(impDir.getDirPreImport());
				List<File> files = (List<File>) FileUtils.listFiles(importFolder, extensions, false);
				if (files.size() == 0) {
					CommonVars.logger.info("CapWeb : No TOP file Found ...");
					Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
					continue;
				}else {
					CommonVars.logger.info("CapWeb : "+files.size()+" to process");
				}
				CommonVars.logger.info("CapWeb - IMPORT_TELECOLLECTE_OK : " + Main.IMPORT_TELECOLLECTE_OK);
				if (CommonVars.checkIfCanContinue() && Main.IMPORT_TELECOLLECTE_OK) {
					for (File topfile : files) {
						if (!topfile.getName().matches("\\d{38}\\.[tT][Oo][pP]")) {
							CommonVars.logger.warn("!!! Insupported file[" + topfile.getName() + "]");
							Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
						} else if (!CommonVars.hMapExist(topfile.getName().substring(0, topfile.getName().length() - 4))) {
							CommonVars.logger.info("Available Threads [" + mySem.availablePermits() + "]");
							try {
								mySem.acquire();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							CommonVars.hMapAdd(topfile.getName().substring(0, topfile.getName().length() - 4));
							(new DoWorkEncrichCapWeb(topfile.getName().substring(0, topfile.getName().length() - 4))).start();
						}
					}
				} else {
					Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
				}
				while (mySem.availablePermits() != impDir.getNbMaxThread()) {
					// CommonVars.logger.warn("#ThreadCapWeb.wait ################# "+CommonVars.THREAD_SLEEP_TIME+" #################");
					Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
				}
			}
			CommonVars.logger.warn("#ThreadCapWeb.stop ###########################################################");
		} catch (Exception e) {
			CommonVars.logger.error("#ThreadCapWeb.run#", e);
		} finally {
			while (mySem.availablePermits() < impDir.getNbMaxThread()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		CommonVars.logger.info("End ThreadCapWeb.run");
	}

	/**
	 * thread de traiment des grains capture web
	 * 
	 * @author K.ABIDA
	 *
	 */
	private class DoWorkEncrichCapWeb extends Thread {
		private String fileLot;
		private String newFileName;

		/**
		 * Contructeur d'objet
		 * 
		 * @param fileLotIn le nom du fichier grains
		 */
		public DoWorkEncrichCapWeb(String fileLotIn) {
			fileLot = fileLotIn;

		}

		/**
		 * Demarage du thread de traiment 
		 */
		public void run() {
			CommonVars.logger.info("Start DoWorkEncrichCapWeb.run");
			try {
				String erreurDetail = null;
				CommonVars.logger.info("Lot to treat : [" + fileLot + "]");
				Date startDate = new Date();
				ArrayList<String> dataNewFile = null;
				boolean isOk = !CommonVars.isFileAlreadyExist(fileLot);
				if (!isOk) {
					erreurDetail = "CapWeb - File already Imported !!!";
				}else {
					CommonVars.logger.info("Start processing CapWeb : [" + fileLot + "]");
				}
				EnrichWorkCapWeb enrichWork = null;
				if (isOk) {
					enrichWork = new EnrichWorkCapWeb(fileLot, impDir.getDirPreImport());
					isOk = enrichWork.doWorkForCapWeb();
					if (isOk) {
						dataNewFile = enrichWork.dataNewFile;
					}else {
						CommonVars.logger.error("#ThreadCapWeb.DoWorkEncrichCapWeb# Fails : "+enrichWork.erreurDetail);
					}
					erreurDetail = enrichWork.erreurDetail;
				}
				moveFilesToDir(isOk, dataNewFile, enrichWork);
				CommonVars.insertFileNameDB(enrichWork == null ? "ERROR" : enrichWork.source, fileLot, this.newFileName,
						isOk ? 1 : 0, PCHDateFunc.getDiffDateSecFromNow(startDate),
						enrichWork == null ? -1 : enrichWork.docCount, erreurDetail);
				CommonVars.hMapDelete(this.fileLot);
			} catch (Exception e) {
				CommonVars.logger.error("#ThreadCapWeb.DoWorkEncrichCapWeb#", e);
			} finally {
				mySem.release();
			}
			CommonVars.logger.info("End DoWorkEncrichCapWeb.run");
		}

		/**
		 * deplace les fichiers grains apres traitement
		 * @param isSuccess specifie si le traitement est ok ou KO
		 * @param dataNewFile le nouveau contenue du fichier .lot apres enrichissement 
		 * @param enrichWork objet qui contient les donn�es apres enrichissement
		 * @return true si ok false sinon
		 */
		private boolean moveFilesToDir(boolean isSuccess, ArrayList<String> dataNewFile, EnrichWorkCapWeb enrichWork) {
			boolean isOk = true;
			CommonVars.logger.info("Start DoWorkEncrichCapWeb.moveFilesToDir");
			newFileName = fileLot;
			try {
				if (isSuccess) {
					BufferedWriter writer = new BufferedWriter(
							new FileWriter(impDir.getDirPreImport() + this.fileLot + "_inwork.lot"));
					for (String line : dataNewFile) {
						writer.write(line + "\n");
					}
					writer.close();
					if (enrichWork.isNewImagesFile()) {
						try {
							
							
							 Files.write(Paths.get(impDir.getDirPreImport() + this.fileLot + ".pak"),
							 enrichWork.getFileColorContent(), StandardOpenOption.CREATE);
							 

							Files.write(Paths.get(impDir.getDirPreImport() + this.fileLot + ".jpk"),
									enrichWork.getNewImagesColor(), StandardOpenOption.APPEND);
						} catch (Exception e) {
							CommonVars.logger.error("Can't write images files [" + fileLot + "]", e);
							isSuccess = false;
							Files.delete(Paths.get(impDir.getDirPreImport() + this.fileLot + "_inwork.lot"));
						}
					}
				}
				String pathImport = impDir.getDirImport();
				String pathPreImport = impDir.getDirPreImport();
				if (isSuccess) {
					boolean isMoved = CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".lot"),
							pathPreImport + "TRAITE/", null, true);
					if (isMoved) {
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + "_inwork.lot"), pathImport,
								new File(pathImport + this.fileLot + ".lot"), true);
						//(new File(pathPreImport + this.fileLot + ".pak")).delete();
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".jpk"), pathImport, null, true);
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".pak"), pathImport, null, true);
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".top"), pathImport, null, true);
					} else {
						CommonVars.logger.error("Can't remove files [" + fileLot + "] [" + pathImport + this.fileLot
								+ ".lot" + "]  to TRAITE");
						isOk = false;
					}
				} else {
					boolean isMoved = CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".lot"),
							pathPreImport + "REJECT/", null, true);
					if (isMoved) {
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".pak"), pathPreImport + "REJECT/",
								null, true);
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".jpk"), pathPreImport + "REJECT/",
								null, true);
						CommonVars.moveFile(new File(pathPreImport + this.fileLot + ".top"), pathPreImport + "REJECT/",
								null, true);
					} else {
						CommonVars.logger.error("Can't remove files [" + fileLot + "] [" + impDir.getDirPreImport()
								+ "REJECT/" + this.fileLot + ".lot" + "] to REJECT");
						isOk = false;
					}
				}
			} catch (Exception ee) {
				CommonVars.logger.error("# DoWorkEncrichCapWeb.moveFilesToDir#", ee);
				isOk = false;
			} finally {
				CommonVars.logger.info("End DoWorkEncrichCapWeb.moveFilesToDir [" + isOk + "]");
			}

			return isOk;
		}
	}
}
