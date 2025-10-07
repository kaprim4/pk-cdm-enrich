package procheck.dev.enrich.document;

import procheck.dev.enrich.CMC7ExtractorService;
import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.enrich.tools.CMC7Tools;
import procheck.dev.lib.PCHStringOpr;
import procheck.dev.lib.ai.PKICRAIMontant;
import procheck.dev.lib.ai.PKOCRCMC7;

import java.util.*;

/**
 * Représente une valeur extraite d'un fichier .lot avec ses données d'enrichissement
 */
public class Valeur {

	private final String lineToAnalyse;
	private String dataEnrich;
	private String mntICR = "";
	private String mntLettreICR = "";
	private final String mntInitial;
	private final String mntModifier;
	private final String cmc7FinalCap;
	String IsRectoWrite;
	String ISEndroitWrite;

	private String cmc7Magnetique;

	public Valeur(String line) {
		this.lineToAnalyse = line.length() >= 869 ? line.substring(0, 719) : line;
		this.dataEnrich = CommonVars.HEAD_DATA_DOC;
		this.mntInitial = line.length() >= 869 ? line.substring(851, 869).trim() : "";
		this.mntModifier = line.length() >= 832 ? line.substring(814, 832).trim() : "";
		this.cmc7FinalCap = line.length() >= 770 ? line.substring(730, 770).trim() : "";
		this.IsRectoWrite = line.length() >= 827 ? line.substring(826, 827).trim() : "0";
		this.ISEndroitWrite = line.length() >= 829 ? line.substring(828, 829).trim() : "0";
	}

	public boolean doWork(PCDocument document) {
		boolean isOk = true;
		try {
			dataEnrich = CommonVars.insertStringToString(dataEnrich, "03", 11);
			String cmc7Toanalyse = lineToAnalyse.substring(9, 73);
			if(!cmc7Toanalyse.trim().isEmpty()) {
				cmc7Magnetique = CMC7Tools.getCMC7Data(cmc7Toanalyse);
			}
			document.setCmc7FromRectified(cmc7Magnetique);

			CommonVars.logger.info("###################  FUSION ["+cmc7Magnetique+"] START ###################");

			String cmc7FromDNG = CMC7Tools.getCMC7FromDNG(document, false, false, false);
			String fusionStep1 = cmc7FromDNG;
			boolean cmc7InVerso = false;

			if (cmc7FromDNG.trim().isEmpty()) {
				fusionStep1 = CMC7Tools.getCMC7FromDNG(document, false, true, true);
				if (fusionStep1.length() > 30) cmc7InVerso = true;
			}
			document.setCmc7FromOCRServer("DNG-" + (cmc7InVerso ? "V" : "R") + fusionStep1);

			boolean isRIBStep1Valid = CMC7ExtractorService.isCorrectRIBKeyFromCMC7(fusionStep1);
			CommonVars.logger.info("[FUSION 1] Scanner vs DNG: " + fusionStep1 + " | RIB Valid: " + isRIBStep1Valid);

			if (!isRIBStep1Valid) {
				fusionStep1 = CMC7ExtractorService.simpleFusion(cmc7Magnetique, cmc7FromDNG);
				CommonVars.logger.info("[FUSION 1] Résultat de fusion simple: " + fusionStep1);
			}
			document.setCmc7Final(fusionStep1);

			String fusionStep2 = fusionStep1;
			if (!CMC7ExtractorService.isCorrectRIBKeyFromCMC7(fusionStep1)) {
				byte[] image = cmc7InVerso ? document.getImageColorRear() : document.getImageColorFront();
				String cmc7FromAI = CMC7Tools.getCMC7FromrAI(image, CommonVars.TYPE_VALEUR);
				document.setCmc7FromAIServer(cmc7FromAI);

				boolean isRIBAIValid = CMC7ExtractorService.isCorrectRIBKeyFromCMC7(cmc7FromAI);
				CommonVars.logger.info("[FUSION 2] DNG vs AI: " + cmc7FromAI + " | RIB Valid: " + isRIBAIValid);

				fusionStep2 = isRIBAIValid ? cmc7FromAI : CMC7ExtractorService.simpleFusion(fusionStep1, cmc7FromAI);
				CommonVars.logger.info("[FUSION 2] Résultat de fusion: " + fusionStep2);
				document.setCmc7Final(fusionStep2);
			}

			String fusionStep3 = fusionStep2;
			if (!CMC7ExtractorService.isCorrectRIBKeyFromCMC7(fusionStep2)) {
				fusionStep3 = CMC7ExtractorService.intelligentFusion(cmc7Magnetique, cmc7FromDNG, fusionStep2);
				CommonVars.logger.info("[FUSION 3] Fusion intelligente: " + fusionStep3);
				document.setCmc7Final(fusionStep3);
			}

			String fusionStep4 = fusionStep3;
			if (!CMC7ExtractorService.isCorrectRIBKeyFromCMC7(fusionStep4)) {
				List<String> candidates = Arrays.asList(fusionStep1, fusionStep2, fusionStep4);
				String bestCandidate = CMC7ExtractorService.findBestCandidate(candidates);
				CommonVars.logger.info("[FUSION 4] Candidat retenu: " + bestCandidate);

				if (CMC7ExtractorService.isCorrectRIBKeyFromCMC7(bestCandidate)) {
					fusionStep4 = bestCandidate;
					CommonVars.logger.info("[FUSION 4] FinalCMC7 remplacé : " + fusionStep4);
				} else {
					CommonVars.logger.warn("[FUSION 4] Aucun candidat CMC7 valide trouvé.");
				}
			}

			document.setCmc7Final(fusionStep4);
			if (!CMC7ExtractorService.isCorrectRIBKeyFromCMC7(fusionStep4)) {
				document.setCmc7Final(fusionStep1);
				CommonVars.logger.warn("Si Aucun candidat CMC7 valide trouvé set resultat de fusion 1 ["+fusionStep1+"]");
			}
			dataEnrich = CommonVars.insertStringToString(dataEnrich, PCHStringOpr.rightPad(document.getCmc7Final(), " ", 64), 13);
			CommonVars.logger.info("###################  FUSION END ###################");
			if (CommonVars.checkIfICRActive()) {
				PKICRAIMontant icr = new PKICRAIMontant();
				if (icr.doWork(CommonVars.ICR_AI_URL, document.getImageColorFront())) {
					mntICR = icr.getDataICR().mntC;
					mntLettreICR = icr.getDataICR().mntL;
					if (mntICR != null && mntICR.equals(mntLettreICR) && !mntICR.isEmpty()) {
						document.probability = 1;
					}
					document.mntICRHisto = mntICR;
					document.mntLettreICRHisto = mntLettreICR;
				} else {
					CommonVars.logger.error(icr.getErrorCode());
				}
			}

			String isSigne = "0";
			String dateEmission = "";
			if (CommonVars.ICR_FF_IS_ACTIVE) {
				isSigne = extractFieldFromOCR(CommonVars.ICR_AI_SIGN, document).contains("OUI") ? "1" : "0";
				dateEmission = extractFieldFromOCR(CommonVars.ICR_AI_DATE, document);
			}

			if (cmc7Toanalyse.trim().chars().filter(Character::isDigit).count() > 10) 
			    IsRectoWrite = "1";
			 else 
			    IsRectoWrite = "0";
			
			if(cmc7Toanalyse.trim().length()>15) 
				ISEndroitWrite = "1";
			else
				ISEndroitWrite ="0";


			dataEnrich = PCHStringOpr.rightPad(dataEnrich, " ", 107);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + IsRectoWrite, " ", 109);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + ISEndroitWrite, " ", 111);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + mntICR, " ", 114);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + isSigne + " " + dateEmission, " ", 132);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + mntInitial, " ", 150);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + mntModifier, " ", 168);
			dataEnrich = PCHStringOpr.rightPad(dataEnrich + cmc7FinalCap, " ", 232);

		} catch (Exception e) {
			CommonVars.logger.error("#Valeur.doWork#", e);
			isOk = false;
		}
		return isOk;
	}

	private String extractFieldFromOCR(String endpoint, PCDocument doc) {
		PKOCRCMC7 ocr = new PKOCRCMC7();
		return ocr.doWork(endpoint, doc.getImageColorFront(), "") ? ocr.getDataOCR().cmc7 : "";
	}

	public String getDataEnrich() {
		return dataEnrich;
	}
}