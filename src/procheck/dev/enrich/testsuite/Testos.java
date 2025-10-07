package procheck.dev.enrich.testsuite;

import java.io.File;

import javax.annotation.RegEx;

import org.apache.commons.lang3.StringUtils;

import com.google.common.primitives.Bytes;

public class Testos {
	public static void main(String[] args) {
/*
		String CMC7Valide = getCMC7Data("034 1074526 190780 2111118133900003 69 ");
		System.out.println("CMC7[" + CMC7Valide + "]");
		System.out.println("RIB KEY Check : ["+isCorrectRIBKeyFromCMC7(CMC7Valide)+"]");*/
		System.out.println("RIB KEY Check : ["+isCorrectRIBKey("011738000003210006132624")+"]");
	}

	public static String getCMC7Data(String cmc7Scanner) {
		String retVal = "";
		try {
			cmc7Scanner = cmc7Scanner.trim();
			cmc7Scanner = cmc7Scanner.replace("<", ";");
			cmc7Scanner = cmc7Scanner.replace(">", ";");
			cmc7Scanner = cmc7Scanner.replace(":", "");
			cmc7Scanner = cmc7Scanner.replace(" ", ";");
			
			String reg = "[0-9? ]{0,3};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			String regCentral = "[0-9?]{10};[0-9?]{7};[0-9?]{6};[0-9?]{16};[0-9?]{2}";
			
			if(cmc7Scanner.substring(0,1).equals(";")) {
				cmc7Scanner = cmc7Scanner.substring(1);
			}
			int lngZ1 = cmc7Scanner.indexOf(";"), idx = 3;
			while ((idx - lngZ1) > 0) {
				cmc7Scanner = " " + cmc7Scanner;
				idx--;
			}
			
			if(cmc7Scanner.matches(regCentral)) {
				cmc7Scanner = cmc7Scanner.substring(0,3)+";"+cmc7Scanner.substring(3);
			}
			if(cmc7Scanner.matches(reg)) {
				retVal=cmc7Scanner;	
			}
		} catch (Exception e) {
			retVal = "";
		}
		return retVal;
	}
	public static boolean isCorrectRIBKeyFromCMC7(String CMC7) {
		if (CMC7 == null || CMC7.length() < 37 || CMC7.length() < 38) {
			return false;
		}
		String cmc7Data[] = CMC7.split(";");
		String rib24 = cmc7Data[2] + cmc7Data[3] + cmc7Data[4];
		if (rib24 == null || rib24.length() != 24) {
			return false;
		}
		long rib1 = 0;
		long rib2 = 0;
		long res1 = 0;
		long rib3 = 0;
		long res2 = 0;
		long clec = 0;

		rib1 = Long.parseLong(rib24.substring(0, 12));
		rib2 = Long.parseLong((rib24.substring(12, 22) + "00"));
		res1 = rib1 % 97;
		String sRES1 = String.valueOf(res1);
		while (sRES1.length() < 2) {
			sRES1 = "0" + sRES1;
		}
		String sRIB2 = String.valueOf(rib2);
		while (sRIB2.length() < 12) {
			sRIB2 = "0" + sRIB2;
		}
		rib3 = Long.parseLong((sRES1 + sRIB2));
		res2 = rib3 % 97;
		clec = 97 - res2;
		String sCLEC = String.valueOf(clec);
		while (sCLEC.length() < 2) {
			sCLEC = "0" + sCLEC;
		}
		if (rib24.subSequence(22, 24).equals(sCLEC)) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean isCorrectRIBKey(String rib24) {

		if (rib24 == null || rib24.length() != 24) {
			return false;
		}
		long rib1 = 0;
		long rib2 = 0;
		long res1 = 0;
		long rib3 = 0;
		long res2 = 0;
		long clec = 0;

		rib1 = Long.parseLong(rib24.substring(0, 12));
		rib2 = Long.parseLong((rib24.substring(12, 22) + "00"));
		res1 = rib1 % 97;
		String sRES1 = String.valueOf(res1);
		while (sRES1.length() < 2) {
			sRES1 = "0" + sRES1;
		}
		String sRIB2 = String.valueOf(rib2);
		while (sRIB2.length() < 12) {
			sRIB2 = "0" + sRIB2;
		}
		rib3 = Long.parseLong((sRES1 + sRIB2));
		res2 = rib3 % 97;
		clec = 97 - res2;
		String sCLEC = String.valueOf(clec);
		while (sCLEC.length() < 2) {
			sCLEC = "0" + sCLEC;
		}
		if (rib24.subSequence(22, 24).equals(sCLEC)) {
			return true;
		} else {
			return false;
		}
	}
}
