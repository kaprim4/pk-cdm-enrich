package procheck.dev.enrich.threads.gab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;
import org.jcodec.common.logging.Logger;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;
import procheck.dev.enrich.ocr.ServerConfig;
import procheck.dev.lib.PCHDateFunc;
import procheck.dev.lib.PCHStringOpr;

/**
 * Traitement Master des grains de type GAB
 * 
 * @author K.ABIDA
 *
 */
public class ThreadGAB extends Thread {
	/**
	 * Objet qui contient la configuration de l'import
	 */
	private ServerConfig.ImportDirectory impDir;
	/**
	 * un semaphore pour limter le nombre de traitement un parallele des grains
	 */
	public static Semaphore mySem;

	/**
	 * Constructeur d'objet
	 * 
	 * @param impDirIn parametrage de l'import
	 */
	public ThreadGAB(ServerConfig.ImportDirectory impDirIn) {
		impDir = impDirIn;
		mySem = new Semaphore(impDir.getNbMaxThread(), true);
	}

	/**
	 * demarrage du thread
	 */
	public void run() {
	    CommonVars.logger.info("Start ThreadGAB.run");
	    try {
	        while (true && !Main.STOP_ME) {
	            String[] extensions = new String[]{"zip", "ZIP", "Zip", "taupe"};
	            File importFolder = new File(impDir.getDirPreImport());
	            List<File> files = (List<File>) FileUtils.listFiles(importFolder, extensions, false);
	            if (files.size() == 0) {
	                CommonVars.logger.info("No Gab file Found ...");
	                Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
	                continue;
	            }
	            CommonVars.logger.info("IMPORT_TELECOLLECTE_OK : " + Main.IMPORT_TELECOLLECTE_OK);
	            if (CommonVars.checkIfCanContinue() && Main.IMPORT_TELECOLLECTE_OK) {
	                // Group files by the common part of their filenames
	                Map<String, List<File>> fileGroups = new HashMap<>();
	                for (File file : files) {
	                    String fileName = file.getName();
	                    if(fileName.contains("taupe")) {
	                    	CommonVars.logger.info("Top file found: " + fileName); // Log filename being checked
	                    	continue;
	                    }
	                    CommonVars.logger.info("Checking file: " + fileName); // Log filename being checked
	                    // Verify the filename with the regex pattern
	                    if (!fileName.matches("(?i)^\\d{5}-\\d{5}-\\d{3}-\\d{4}-\\d{2}-\\d{2}\\.\\d{6}\\.\\d{3}\\.cop\\.(img|xml)\\.zip$")) {
	                        CommonVars.logger.warn("!!! Unsupported file (regex mismatch) [" + fileName + "]");
	                        continue;
	                    }
	                    String baseName = fileName.substring(0, fileName.indexOf(".cop."));
	                    String topFileName = baseName + ".cop.xml.taupe";
	                    File topFile = new File(importFolder, topFileName);
	                    
	                    // Check if the topFile exists in the directory
	                    if (topFile.exists()) {
	                        fileGroups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(file);
	                    } else {
	                        CommonVars.logger.warn("!!! Top file not found for base name [" + baseName + "]");
	                    }
	                }

	                // Iterate over grouped files
	                for (Map.Entry<String, List<File>> entry : fileGroups.entrySet()) {
	                	CommonVars.logger.debug("Start Process [" + entry.getKey() + "]");
	                    List<File> group = entry.getValue();
	                    if (group.size() > 1) {
	                        File imgFile = group.stream().filter(f -> f.getName().endsWith(".cop.img.zip")).findFirst().orElse(null);
	                        File xmlFile = group.stream().filter(f -> f.getName().endsWith(".cop.xml.zip")).findFirst().orElse(null);

	                        if (imgFile != null && xmlFile != null) {
	                            // Check if both files are ready
	                            if (CommonVars.fileIsReadyToUse(imgFile) && CommonVars.fileIsReadyToUse(xmlFile)) {
	                                CommonVars.logger.info("Available Threads [" + mySem.availablePermits() + "]");
	                                try {
	                                    mySem.acquire();
	                                } catch (InterruptedException e) {
	                                    e.printStackTrace();
	                                }

	                                // String baseName = imgFile.getName().substring(0, imgFile.getName().lastIndexOf(".cop."));
	                                CommonVars.hMapAdd(entry.getKey());
	                                (new DoWorkEncrichGAB(xmlFile.getName(), imgFile.getName())).start();
	                            } else {
	                                CommonVars.logger.info("File(s) not ready to use !!!! [" + imgFile.getName() + ", " + xmlFile.getName() + "]");
	                            }
	                        }
	                    } else {
	                        if (group.size() > 0) {
	                            for (File file : group) {
	                                CommonVars.logger.warn("!!! Unsupported file (incomplete group) [" + file.getName() + "]");
	                            }
	                        }
	                    }
	                }
	            } else {
	                Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
	            }
	            Thread.sleep(CommonVars.THREAD_SLEEP_TIME);
	        }
	    } catch (Exception e) {
	        CommonVars.logger.error("#ThreadGAB.run#", e);
	    } finally {
	        while (mySem.availablePermits() < impDir.getNbMaxThread()) {
	            try {
	                Thread.sleep(1000);
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    CommonVars.logger.info("End ThreadGAB.run");
	}




	/**
	 * thread de traiment des grains GAB
	 * 
	 * @author K.ABIDA
	 *
	 */
	private class DoWorkEncrichGAB extends Thread {
		/**
		 * le nom du fichier lot
		 */
		private String xmlFileName;
		/**
		 * le nom du fichier lot
		 */
		private String imgFileName;
		/**
		 * le nom du nouveau fichier grains
		 */
		private String newFileName;

		/**
		 * Le nombre de valeurs
		 */
		private int docCount;

		/**
		 * Constructeur d'objet
		 * 
		 * @param fileLotIn le nom du fichier grain
		 */
		public DoWorkEncrichGAB(String xmlFileName, String imgFileName) {
			this.xmlFileName = xmlFileName;
			this.imgFileName = imgFileName;
			docCount = 0;
		}

		/**
		 * Demarrage du thread de traitement
		 */
		public void run() {
			CommonVars.logger.info("Start run.DoWorkEncrichGAB");
			try {
				String erreurDetail = null;
				CommonVars.logger.info("GAB zip lot : [" + xmlFileName + " & " + imgFileName + "]");
				String lotName = xmlFileName.substring(0, xmlFileName.indexOf(".cop."));
				Date startDate = new Date();
				ArrayList<String> dataNewFile = null;
				boolean isOk = !CommonVars.isFileAlreadyExist(lotName);

				EnrichWorkGAB enrichWork = null;
				if (isOk) {
					enrichWork = new EnrichWorkGAB(xmlFileName, imgFileName, impDir.getDirPreImport());
					isOk = enrichWork.doWorkForGAB();
					if (isOk) {
						dataNewFile = enrichWork.dataNewFile;
						docCount = enrichWork.docCount;
					} else {
						erreurDetail = enrichWork.erreurDetail;
						CommonVars.logger.error("#ThreadGAB.run ["+erreurDetail+"]");
					}
				} else {
					erreurDetail = "File already exist in DB";
				}
				moveFilesToDir(isOk, dataNewFile, enrichWork);
				
				CommonVars.insertFileNameDB("CAPGB", lotName, this.newFileName, isOk ? 1 : 0, PCHDateFunc.getDiffDateSecFromNow(startDate), docCount, erreurDetail);
				CommonVars.hMapDelete(lotName);
			} catch (Exception e) {
				CommonVars.logger.error("#run.DoWorkEncrichGAB#", e);
			} finally {
				mySem.release();
			}
			CommonVars.logger.info("End run.DoWorkEncrichGAB");
		}

		/**
		 * deplace les fichiers grains apres traitement
		 * 
		 * @param isSuccess   specifie si le traitement est ok ou KO
		 * @param dataNewFile le nouveau contenue du fichier .lot apres enrichissement
		 * @param enrichWork  objet qui contient les donn�es apres enrichissement
		 * @return true si ok false sinon
		 */
		private boolean moveFilesToDir(boolean isSuccess, ArrayList<String> dataNewFile, EnrichWorkGAB enrichWork) {
			boolean isOk = true;
			CommonVars.logger.info("Start DoWorkEncrichGAB.moveFilesToDir");
			try {
				String importpath = impDir.getDirImport();
				String preImportPath = impDir.getDirPreImport();
				newFileName = getLotNewName(xmlFileName);
				if (isSuccess) {
					BufferedWriter writer = new BufferedWriter(
							new FileWriter(preImportPath + newFileName + "_inwork.lot"));
					for (String line : dataNewFile) {
						writer.write(line + "\n");
					}
					writer.close();
					try {
						// /*
						 Files.write(Paths.get(preImportPath + newFileName + ".pak"), enrichWork.getNewImagesBW(), StandardOpenOption.CREATE);						 
						// */
						Files.write(Paths.get(preImportPath + newFileName + ".jpk"), enrichWork.getNewImagesColor(), StandardOpenOption.CREATE);
					} catch (Exception e) {
						CommonVars.logger.error("Can't write images files [" + imgFileName + "]", e);
						isSuccess = false;
						Files.delete(Paths.get(preImportPath + newFileName + "_inwork.lot"));
					}
				}

				if (isSuccess) {
					
					boolean isMovedXML = CommonVars.moveFile(new File(preImportPath + this.xmlFileName), preImportPath + "TRAITE/", null, true);
					boolean isMovedIMG = CommonVars.moveFile(new File(preImportPath + this.imgFileName), preImportPath + "TRAITE/", null, true);
                    String topFileName = this.xmlFileName.substring(0, this.xmlFileName.indexOf(".zip"));
					CommonVars.moveFile(new File(preImportPath + topFileName + ".taupe"), preImportPath + "TRAITE/", null, true);
					CommonVars.logger.debug("Top moved to TRAITE ["+preImportPath + topFileName + ".taupe"+"]");
					if (isMovedXML && isMovedIMG) {
						CommonVars.moveFile(new File(preImportPath + newFileName + "_inwork.lot"), importpath, new File(importpath + newFileName + ".lot"), true);
						CommonVars.moveFile(new File(preImportPath + newFileName + ".jpk"), importpath, null, true);
						CommonVars.moveFile(new File(preImportPath + newFileName + ".pak"), importpath, null, true);
						BufferedWriter writerTop = null;
						writerTop = new BufferedWriter(new FileWriter(importpath + newFileName + ".top"));
						writerTop.write("TOP FILE CREATED By Procheck:");
						if (writerTop != null)
							writerTop.close();
					} else {
						CommonVars.logger.error("Can't remove IMG file [" + imgFileName + "] [" + importpath + this.imgFileName + "]");
						CommonVars.logger.error("Can't remove XML file [" + xmlFileName + "] [" + importpath + this.xmlFileName + "]");
						isOk = false;
						if(isMovedXML)
							CommonVars.moveFile(new File(preImportPath + "TRAITE/" + this.xmlFileName), preImportPath+ "REJECT/", null, true);
						if(isMovedIMG)
							CommonVars.moveFile(new File(preImportPath + "TRAITE/" + this.imgFileName), preImportPath+ "REJECT/", null, true);
					}
				} else {
					boolean isMovedIMG = CommonVars.moveFile(new File(preImportPath + this.imgFileName), preImportPath + "REJECT/", null, true);
					boolean isMovedXML = CommonVars.moveFile(new File(preImportPath + this.xmlFileName), preImportPath + "REJECT/", null, true);
					String topFileName = this.xmlFileName.substring(0, this.xmlFileName.indexOf(".zip"));
					CommonVars.moveFile(new File(preImportPath + topFileName + ".taupe"), preImportPath + "REJECT/", null, true);
					CommonVars.logger.debug("Top moved to REJECT ["+preImportPath + topFileName + ".taupe"+"]");
					if (!isMovedIMG) {
						CommonVars.logger.error("Can't remove IMG file [" + imgFileName + "] [" + preImportPath + "REJECT/" + this.imgFileName + "] to REJECT");
						isOk = false;
					}
					if (!isMovedXML) {
						CommonVars.logger.error("Can't remove XML file [" + xmlFileName + "] [" + preImportPath + "REJECT/" + this.xmlFileName + "] to REJECT");
						isOk = false;
					}
				}
			} catch (Exception ee) {
				CommonVars.logger.error("# DoWorkEncrichGAB.moveFilesToDir#", ee);
				isOk = false;
			} finally {
				CommonVars.logger.info("End DoWorkEncrichGAB.moveFilesToDir [" + isOk + "]");
			}
			return isOk;
		}
	}

	/**
	 * Retourne le nouveau nom de fichier grains
	 * 
	 * @param fileNameGAB le nom duchier grain GAB
	 * @return le nouveau nom du fichier grain
	 */
	private String getLotNewName(String fileNameGAB) {
		Logger.info("getLotNewName.START");
		String newName = null;
		String timeTrt = PCHDateFunc.getDateTimeyyMMddHHmmss();
		//newName = timeTrt + "00021" +"00"+ fileNameGAB.substring(12, 15) + "005" + fileNameGAB.substring(6, 11)+ getRandomNum8();
		Logger.info("["+timeTrt+" | 00021 | " + fileNameGAB.substring(6, 11) +" | "+ fileNameGAB.substring(12, 15) +" | "+ fileNameGAB.substring(6, 11)+" | "+ getRandomNum8());
		newName = timeTrt + "00021" + fileNameGAB.substring(6, 11) + fileNameGAB.substring(12, 15) + fileNameGAB.substring(6, 11)+ getRandomNum8();
		return newName;
	}

	/**
	 * retourne une sequence aleatoire
	 * 
	 * @return la sequence aleatoire sur 8 positions
	 */
	public static String getRandomNum8() {
		double d = Math.random();
		String randomValue = (d + "").replace(",", "").replace(".", "");
		randomValue = PCHStringOpr.leftPad(randomValue, "0", 8);
		return randomValue;
	}
}
