package procheck.dev.enrich.tools;

import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.Sanselan;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.cmc7.Cmc7Work;
import procheck.dev.enrich.threads.PCDocument;

/**
 * Des outils pour l'utilisations du CMC7
 * @author K.ABIDA
 *
 */
public class CMC7Tools {
	
	/**
	 * manipulation de la CMC7 lu par le scanner
	 * @param cmc7Scanner la CMC7 envoy�e par le scanner
	 * @return CMC7 apres rectification
	 */
	private static int DPI = Integer.parseInt(CommonVars.DPI);
	public static String getCMC7Data(String cmc7Scanner) {
		CommonVars.logger.info("CMC7Tools.getCMC7Data START cmc7Scanner : ["+cmc7Scanner+"]");
		
		if(cmc7Scanner.trim().isEmpty()) {
			return cmc7Scanner;
		}
		
		if (cmc7Scanner.matches(".*[A-Za-z].*")) {
			CommonVars.logger.info("Contains an alphabet(s)");
			cmc7Scanner = cleanCMC7(cmc7Scanner);
			CommonVars.logger.info("cmc7Scanner Cleaned: ["+cmc7Scanner+"]");
        }else {
        	CommonVars.logger.info("NOT Contains an alphabet(s)");
        }
		
		String retVal = "";
		try {
			cmc7Scanner = cmc7Scanner.trim();
			cmc7Scanner = cmc7Scanner.replace("<", ";");
			cmc7Scanner = cmc7Scanner.replace(">", ";");
			cmc7Scanner = cmc7Scanner.replace(":", "");
			cmc7Scanner = cmc7Scanner.replace(" ", ";");

			String regTemp1 = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{3}";
			String regoOldCmc7 = "[0-9? ]{0,3};[0-9?]{8};[0-9?]{6};[0-9?]{16};[0-9?]{3}";
			String reg = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regCentral = "[0-9?]{10};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regWithoutZ1 = "[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regCmc7Overlowed10 = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16,18};[0-9?]{2}";

			if (cmc7Scanner.substring(0, 1).equals(";")) {
				cmc7Scanner = cmc7Scanner.substring(1);
			}
			int lngZ1 = cmc7Scanner.indexOf(";"), idx = 3;
			while ((idx - lngZ1) > 0) {
				cmc7Scanner = " " + cmc7Scanner;
				idx--;
			}
			if (cmc7Scanner.matches(regoOldCmc7)) {
				retVal = cmc7Scanner.substring(0, 4) + cmc7Scanner.substring(5);
			} else if (cmc7Scanner.matches(regCentral)) {
				retVal = cmc7Scanner.substring(0, 3) + ";" + cmc7Scanner.substring(3);
			} else if (cmc7Scanner.matches(reg)) {
				retVal = cmc7Scanner;
			} else if (cmc7Scanner.matches(regWithoutZ1)) {
				retVal = "   ;" + cmc7Scanner;
			} else if (cmc7Scanner.matches(regCmc7Overlowed10)) {
				retVal = cmc7Scanner.substring(0, 17) + ";" + cmc7Scanner.substring(17).replace(" ","");
			}
			if (cmc7Scanner.matches(regTemp1) && cmc7Scanner.endsWith("?")) {
				retVal = cmc7Scanner.substring(0, cmc7Scanner.length() - 1);
			}
			if (retVal.length()>3 && retVal.substring(0, 3).contains("?")) {
				retVal = retVal.substring(0, 3).replace("?", "0") + retVal.substring(3);
			}
			if(retVal.length()<30) {
				retVal=rectifyCMC7(cmc7Scanner);
			}
		} catch (Exception e) {
			CommonVars.logger.error("CMC7Tool.getCMC7Data "+cmc7Scanner+" CATCHED : ",e);
			e.printStackTrace();
			retVal = "";
		}
		return retVal;
	}
	
	
	public static String cleanCMC7(String cmcc) {
	    CommonVars.logger.info("Valeur.cleanCMC7 START [" + cmcc + "]");
	    String cleanCMC7 = "";
	    try {
	        if (cmcc == null || cmcc.isEmpty()) {
	            return cmcc;
	        }
	        // Extract and clean the required parts (keeping only digits)
	        String first = cmcc.substring(0, 3).replaceAll("[^0-9]", "");
	        String numValeur = cmcc.substring(3, 11).replaceAll("[^0-9]", "");
	        String codeBanqueLocalite = cmcc.substring(11, 18).replaceAll("[^0-9]", "");
	        String numCompte = cmcc.substring(18, 36).replaceAll("[^0-9]", "");
	        String cle = cmcc.substring(36, 39).replaceAll("[^0-9]", "");
	        // Construct the cleaned CMC7 with spaces between parts
	        cleanCMC7 = first + " " + numValeur + " " + codeBanqueLocalite + " " + numCompte + " " + cle;
	        // Ensure final result is exactly 64 characters (padded with spaces)
	        cleanCMC7 = String.format("%-64s", cleanCMC7);
	    } catch (Exception e) {
	        CommonVars.logger.error(e);
	        cleanCMC7 = "";
	    }
	    CommonVars.logger.info("Valeur.cleanCMC7 END [" + cleanCMC7 + "]");
	    return cleanCMC7;
	}

	public static String getCMC7FromDNG(PCDocument document, boolean isBW, boolean rotateImage, boolean sendVerso) {
		String retVal = "";
		CommonVars.logger.info("CMC7Tools.getCMC7Data Start");
		try {
			int dpi = getDpi(document.getImageColorFront());
			if(dpi<DPI) dpi=DPI;

			String cmcFromServer = Cmc7Work.getCMC7FromServer(document, isBW, dpi,rotateImage,sendVerso);
			CommonVars.logger.info("DPI["+dpi+"] | cmcFromServer ["+cmcFromServer+"]");
			if (cmcFromServer == null) {
				return "";
			}

			cmcFromServer = cmcFromServer.trim();
			cmcFromServer = cmcFromServer.replace("<", ";");
			cmcFromServer = cmcFromServer.replace(": ?;", "");
			cmcFromServer = cmcFromServer.replace(": ??", "");
			cmcFromServer = cmcFromServer.replace(": ? ?", "");
			cmcFromServer = cmcFromServer.replace(": ?", "");
			cmcFromServer = cmcFromServer.replace(" ", "?");
			cmcFromServer = cmcFromServer.replace("> ", ">?");
			cmcFromServer = cmcFromServer.replace(">", ";");
			cmcFromServer = cmcFromServer.replace(":", "");
			cmcFromServer = cmcFromServer.replace("=", "?");

			String regTemp1 = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{3}";
			String reg = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regCentral = "[0-9?]{10};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regWithoutZ1 = "[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regoOldCmc7 = "[0-9? ]{0,3};[0-9?]{8};[0-9?]{6};[0-9?]{16};[0-9?]{3}";

			if (cmcFromServer.substring(0, 1).equals(";")) {
				cmcFromServer = cmcFromServer.substring(1);
			}
			int lngZ1 = cmcFromServer.indexOf(";"), idx = 3;
			while ((idx - lngZ1) > 0) {
				cmcFromServer = " " + cmcFromServer;
				idx--;
			}
			if (cmcFromServer.matches(regoOldCmc7)) {
				retVal = cmcFromServer.substring(0, 4) + cmcFromServer.substring(5);
			} else if (cmcFromServer.matches(regCentral)) {
				retVal = cmcFromServer.substring(0, 3) + ";" + cmcFromServer.substring(3);
			} else if (cmcFromServer.matches(reg)) {
				retVal = cmcFromServer;
			} else if (cmcFromServer.matches(regWithoutZ1)) {
				retVal = "   ;" + cmcFromServer;
			}
			if (cmcFromServer.matches(regTemp1) && cmcFromServer.endsWith("?")) {
				retVal = cmcFromServer.substring(0, cmcFromServer.length() - 1);
			}
			if (retVal.substring(0, 3).contains("?")) {
				retVal = retVal.substring(0, 3).replace("?", "0") + retVal.substring(3);
			}
			if(retVal.length()<30) {
				retVal=rectifyCMC7(cmcFromServer);
			}
		} catch (Exception e) {
			retVal = "";
		}
		CommonVars.logger.info("CMC7Tools.getCMC7FromCMC7Server END  [" + retVal + "]");
		return retVal;
	}

	public static String getCMC7FromrAI(byte[] image, String typeValeur) {
		String retVal = "";
		try {
			String cmcFromServer = Cmc7Work.getCMC7FromServerAI(image,typeValeur);
			if (cmcFromServer == null) {
				return "";
			}
			cmcFromServer = cmcFromServer.trim();
			cmcFromServer = cmcFromServer.replace("H", ";");
			String regTemp1 = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{3}";
			String reg = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regCentral = "[0-9?]{10};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regWithoutZ1 = "[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regoOldCmc7 = "[0-9? ]{0,3};[0-9?]{8};[0-9?]{6};[0-9?]{16};[0-9?]{3}";
			//recuperer cmc7 sans la premiere et la derniere ';'
			if (cmcFromServer.substring(0, 1).equals(";")) {
				cmcFromServer = cmcFromServer.substring(1);
				
			}
			if(cmcFromServer.split(";")[0].length()>7)
				cmcFromServer=cmcFromServer.split(";")[0].substring(0,cmcFromServer.split(";")[0].length()-7)+";"+cmcFromServer.split(";")[0].substring(cmcFromServer.split(";")[0].length()-7);
			if(cmcFromServer.endsWith(";")) {
				cmcFromServer=cmcFromServer.substring(0,cmcFromServer.length()-1);	
			}	
			int lngZ1 = cmcFromServer.indexOf(";"), idx = 3;
			while ((idx - lngZ1) > 0) {
				cmcFromServer = " " + cmcFromServer;
				idx--;
			}
			if (cmcFromServer.matches(regoOldCmc7)) {
				retVal = cmcFromServer.substring(0, 4) + cmcFromServer.substring(5);
			} else if (cmcFromServer.matches(regCentral)) {
				retVal = cmcFromServer.substring(0, 3) + ";" + cmcFromServer.substring(3);
			} else if (cmcFromServer.matches(reg)) {
				retVal = cmcFromServer;
			} else if (cmcFromServer.matches(regWithoutZ1)) {
				retVal = "   ;" + cmcFromServer;
			}
			if (cmcFromServer.matches(regTemp1) && cmcFromServer.endsWith("?")) {
				retVal = cmcFromServer.substring(0, cmcFromServer.length() - 1);
			}
			if (retVal.substring(0, 3).contains("?")) {
				retVal = retVal.substring(0, 3).replace("?", "0") + retVal.substring(3);
			}
		} catch (Exception e) {
			CommonVars.logger.error("CMC7Tools.getCMC7FromCMC7ServerAI : ",e);
			retVal = "";
		}
		CommonVars.logger.info("getCMC7FromCMC7ServerAI [" + retVal + "]");
		return retVal;
	}

	public static int getDpi(byte[] img) {
		try {

			ImageInfo imageInfo = Sanselan.getImageInfo(img);
			int physicalWidthDpi = imageInfo.getPhysicalWidthDpi();
			return physicalWidthDpi;
		} catch (Exception e) {
			CommonVars.logger.error("#OCRCMC7.getDpi#", e);
		}
		return 100;
	}
	public static String rectifyCMC7(String cmc7) {
		String retVal="";
		String f6="000";
		if(cmc7.split(";")[0].length()<4)
			f6=cmc7.split(";")[0];
		if(cmc7.charAt(11)==';' && cmc7.charAt(18)=='?') {
			retVal=cmc7.substring(0,18)+";"+cmc7.substring(19);
		}
		if(cmc7.matches(";[0-9?]{7}[?]{1}[0-9?]{6};[0-9?]{16};[0-9?]{2}") ) {
			retVal=cmc7.substring(1,8)+";"+cmc7.substring(9);
		}
		if(cmc7.matches("[0-9?]{3};[0-9?]{7};[0-9?]{6};[0-9?]{16}[?]{1}[0-9?]{2}") ) {
			retVal=cmc7.substring(0,35)+";"+cmc7.substring(36);
		}
		if(cmc7.matches("[0-9?]{3};[0-9?]{7}[?]{1}[0-9?]{6};[0-9?]{16};[0-9?]{2}") ) {
			retVal=cmc7.substring(0,11)+";"+cmc7.substring(12);
		}
		if(cmc7.matches("[0-9?]{3};[0-9?]{7};[0-9?]{6};[0-9?]{18}") ) {
			retVal=cmc7.substring(0,35)+";"+cmc7.substring(35);
		}
		if(cmc7.matches(";[0-9?]{2};[0-9?]{7};[0-9?]{6};[0-9?]{16}[?]{1}[0-9?]{2}") ) {
			retVal=cmc7.substring(1,35)+";"+cmc7.substring(36);
		}
		if(cmc7.matches("[0-9?]{3}[?]{1}[0-9?]{7}[?]{1}[0-9?]{6};[0-9?]{16};[0-9?]{2}") ) {
			retVal=cmc7.substring(0,3)+";"+cmc7.substring(4,11)+";"+cmc7.substring(12);
		}
		if(cmc7.matches("[0-9?]{3}[?]{1}[0-9?]{7};[0-9?]{6};[0-9?]{16}[?]{1}[0-9?]{2}[?]{1}") ) {
			retVal=cmc7.substring(0,3)+";"+cmc7.substring(4,35)+";"+cmc7.substring(36);
		}
		if(cmc7.matches("[0-9?]{2};[0-9?]{7}[?]{1}[0-9?]{6};[0-9?]{16}[?]{1}[0-9?]{2}") ) {
			retVal=cmc7.substring(0,10)+";"+cmc7.substring(11,34)+";"+cmc7.substring(35);
		}
		if(cmc7.matches("[0-9?]{3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2,}") ) {
			retVal=cmc7.substring(4,11)+";"+cmc7.substring(12,18)+";"+cmc7.substring(19,35)+";"+cmc7.substring(36,38);
		}
		if(cmc7.matches("[0-9?]{4,};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}") ) {
			retVal=cmc7.substring(cmc7.length()-38);
		}
		if(retVal.length()>38) {
			retVal="";
		}
		if(retVal.length()==34)
			retVal=f6+";"+retVal;
		return retVal;
	}
}
