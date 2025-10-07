package procheck.dev.enrich;


import java.util.*;
import java.util.function.BiFunction;

public class CMC7ExtractorService {
    // Singleton instance
    private static CMC7ExtractorService instance;

    // Define the lengths of each zone
    private static final int Z1_LENGTH = 2;  // Cle rib length
    private static final int Z2_LENGTH = 16; // Num compt length
    private static final int Z3_LENGTH = 6;  // Code bank + code localite length
    private static final int Z4_LENGTH = 7;  // Num valeur length
    private static final int Z5_LENGTH = 3;  // Usually '000' (3 digits)
    private static final String SEPARATOR = ";";


    // Private constructor to prevent instantiation
    private CMC7ExtractorService() {
    }

    // Get singleton instance
    public static synchronized CMC7ExtractorService getInstance() {
        if (instance == null) {
            instance = new CMC7ExtractorService();
        }
        return instance;
    }

    // CMC7 class to store the extracted zones
    public static class CMC7 {
        private String z1;
        private String z2;
        private String z3;
        private String z4;
        private String z5;

        public CMC7(String cmc7String) {
            String[] zones = cmc7String.split(";");
            if (zones.length == 5) {
                this.z1 = zones[4];
                this.z2 = zones[3];
                this.z3 = zones[2];
                this.z4 = zones[1];
                this.z5 = zones[0];
            } else {
                throw new IllegalArgumentException("CMC7 string must have exactly 5 parts separated by semicolons.");
            }
        }

        public CMC7(String z1, String z2, String z3, String z4, String z5) {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            this.z4 = z4;
            this.z5 = z5;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CMC7 cmc7 = (CMC7) obj;
            return z1.equals(cmc7.z1) && z2.equals(cmc7.z2) && z3.equals(cmc7.z3) && z4.equals(cmc7.z4);
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + z1.hashCode();
            result = 31 * result + z2.hashCode();
            result = 31 * result + z3.hashCode();
            result = 31 * result + z4.hashCode();
            result = 31 * result + z5.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return z5 + ";" + z4 + ";" + z3 + ";" + z2 + ";" + z1;
        }

        public String getZ1() { return z1; }
        public String getZ2() { return z2; }
        public String getZ3() { return z3; }
        public String getZ4() { return z4; }
        public String getZ5() { return z5; }
    }

    // Preprocess IA data (cleaning invalid characters)
    public String preprocessIAData(String IACMC7) {
        IACMC7 = IACMC7.replaceAll("[<>:]", ";"); // Replace separators with ';'
        IACMC7 = IACMC7.replaceAll("\\s", "");    // Remove spaces
        IACMC7 = IACMC7.replaceAll("[^a-zA-Z0-9;]", ""); // Remove non-CMC7 chars
        return IACMC7;
    }

    // Compute the edit distance using dynamic programming (Levenshtein distance)
    public int[][] computeEditDistance(String original, String IA) {
        int m = original.length();
        int n = IA.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (original.charAt(i - 1) == IA.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        return dp;
    }

    // Function to align the two strings based on their edit distance
    public String alignData(String original, String IA) {
        int[][] dp = computeEditDistance(original, IA);
        StringBuilder alignedData = new StringBuilder();

        int i = original.length();
        int j = IA.length();

        while (i > 0 && j > 0) {
            if (original.charAt(i - 1) == IA.charAt(j - 1)) {
                alignedData.insert(0, IA.charAt(j - 1));
                i--;
                j--;
            } else {
                if (dp[i - 1][j] <= dp[i][j - 1] && dp[i - 1][j] <= dp[i - 1][j - 1]) {
                    alignedData.insert(0, '?');
                    i--;
                } else if (dp[i][j - 1] <= dp[i - 1][j] && dp[i][j - 1] <= dp[i - 1][j - 1]) {
                    alignedData.insert(0, IA.charAt(j - 1));
                    j--;
                } else {
                    alignedData.insert(0, '?');
                    i--;
                    j--;
                }
            }
        }

        while (i > 0) {
            alignedData.insert(0, '?');
            i--;
        }
        while (j > 0) {
            alignedData.insert(0, IA.charAt(j - 1));
            j--;
        }

        return alignedData.toString();
    }

    // Full merge: Replace '?' in IA with corresponding val characters
    public CMC7 fusion(CMC7 IACMC7, CMC7 valCMC7) {
        String z1 = mergeZoneFull(IACMC7.z1, valCMC7.z1);
        String z2 = mergeZoneFull(IACMC7.z2, valCMC7.z2);
        String z3 = mergeZoneFull(IACMC7.z3, valCMC7.z3);
        String z4 = mergeZoneFull(IACMC7.z4, valCMC7.z4);
        String z5 = mergeZoneFull(IACMC7.z5, valCMC7.z5);
        return new CMC7(z1, z2, z3, z4, z5);
    }

    // Fusion with custom merge logic via lambda expression
    public CMC7 fusion(CMC7 IACMC7, CMC7 valCMC7, BiFunction<String, String, String> mergeLogic) {
        String z1 = mergeLogic.apply(IACMC7.z1, valCMC7.z1);
        String z2 = mergeLogic.apply(IACMC7.z2, valCMC7.z2);
        String z3 = mergeLogic.apply(IACMC7.z3, valCMC7.z3);
        String z4 = mergeLogic.apply(IACMC7.z4, valCMC7.z4);
        String z5 = mergeLogic.apply(IACMC7.z5, valCMC7.z5);
        return new CMC7(z1, z2, z3, z4, z5);
    }

    private String mergeZoneFull(String IAZone, String valZone) {
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < IAZone.length(); i++) {
            if (IAZone.charAt(i) == '?') {
                merged.append(valZone.charAt(i));
            } else if (IAZone.charAt(i) != valZone.charAt(i)) {
                merged.append('?');
            } else {
                merged.append(IAZone.charAt(i));
            }
        }
        return merged.toString();
    }

    // Extract and align the zones using val semicolon positions
    public CMC7 extractZones(String IACMC7, String valCMC7) {
        // Preprocess IA data
        IACMC7 = preprocessIAData(IACMC7);

        // Align the IA CMC7 to the original val CMC7
        String alignedData = alignData(valCMC7, IACMC7);
        System.out.println("Aligned  IA CMC7\t: " + alignedData);

        // Find semicolon positions in val
        List<Integer> semicolonIndices = new ArrayList<>();
        int idx = valCMC7.indexOf(';');
        while (idx != -1) {
            semicolonIndices.add(idx);
            idx = valCMC7.indexOf(';', idx + 1);
        }
        if (semicolonIndices.size() != 4) {
            throw new IllegalArgumentException("val CMC7 must have exactly 4 semicolons for CMC7 val ["+valCMC7+"]");
        }

        // Extract zones from alignedData based on val semicolon positions
        String z5 = alignedData.substring(0, semicolonIndices.get(0));
        String z4 = alignedData.substring(semicolonIndices.get(0) + 1, semicolonIndices.get(1));
        String z3 = alignedData.substring(semicolonIndices.get(1) + 1, semicolonIndices.get(2));
        String z2 = alignedData.substring(semicolonIndices.get(2) + 1, semicolonIndices.get(3));
        String z1 = alignedData.substring(semicolonIndices.get(3) + 1);

        // Remove any stray semicolons (though unlikely with this approach)
        z1 = z1.replace(";", "");
        z2 = z2.replace(";", "");
        z3 = z3.replace(";", "");
        z4 = z4.replace(";", "");
        z5 = z5.replace(";", "");

        // Ensure correct lengths
        z1 = fixZoneLength(z1, Z1_LENGTH);
        z2 = fixZoneLength(z2, Z2_LENGTH);
        z3 = fixZoneLength(z3, Z3_LENGTH);
        z4 = fixZoneLength(z4, Z4_LENGTH);
        z5 = fixZoneLength(z5, Z5_LENGTH);

        return new CMC7(z1, z2, z3, z4, z5);
    }

    // Helper method to fix zone length
    private String fixZoneLength(String zone, int expectedLength) {
        if (zone.length() > expectedLength) {
            return zone.substring(0, expectedLength);
        } else if (zone.length() < expectedLength) {
            return String.format("%-" + expectedLength + "s", zone).replace(' ', '?');
        }
        return zone;
    }

    // Method to print the extracted CMC7 data in a table format
    public void printCMC7Table(CMC7 cmc7) {
        System.out.printf("%-15s | %-20s | %-35s%n", "Key", "Value", "Description");
        System.out.println("----------------|----------------------|-----------------------------------");
        System.out.printf("%-15s | %-20s | %-35s%n", "Z1", cmc7.z1, "RIB key (2 digits)");
        System.out.printf("%-15s | %-20s | %-35s%n", "Z2", cmc7.z2, "Account number (16 digits)");
        System.out.printf("%-15s | %-20s | %-35s%n", "Z3", cmc7.z3, "Bank code + Locality code (6 digits)");
        System.out.printf("%-15s | %-20s | %-35s%n", "Z4", cmc7.z4, "Value number (7 digits)");
        System.out.printf("%-15s | %-20s | %-35s%n", "Z5", cmc7.z5, "Usually '000' (3 digits)");
    }

    public double calculateAccuracy(String str1, String str2) {
        if (str1.equals(str2)) {
            return 100.0;
        }

        int maxLength = Math.max(str1.length(), str2.length());
        StringBuilder paddedStr1 = new StringBuilder(str1);
        StringBuilder paddedStr2 = new StringBuilder(str2);

        while (paddedStr1.length() < maxLength) {
            paddedStr1.append('?');
        }
        while (paddedStr2.length() < maxLength) {
            paddedStr2.append('?');
        }

        int matchingChars = 0;
        int totalChars = paddedStr1.length();

        for (int i = 0; i < totalChars; i++) {
            if (paddedStr1.charAt(i) != '?' && paddedStr2.charAt(i) != '?' && paddedStr1.charAt(i) == paddedStr2.charAt(i)) {
                matchingChars++;
            }
        }

        return ((double) matchingChars / totalChars) * 100;
    }

    public double calculateAccuracy(CMC7 cmc7Source, CMC7 cmc7Target) {
        List<Double> zoneAccuracies = new ArrayList<>();
        zoneAccuracies.add(calculateAccuracy(cmc7Source.getZ1(), cmc7Target.getZ1()));
        zoneAccuracies.add(calculateAccuracy(cmc7Source.getZ2(), cmc7Target.getZ2()));
        zoneAccuracies.add(calculateAccuracy(cmc7Source.getZ3(), cmc7Target.getZ3()));
        zoneAccuracies.add(calculateAccuracy(cmc7Source.getZ4(), cmc7Target.getZ4()));
        zoneAccuracies.add(calculateAccuracy(cmc7Source.getZ5(), cmc7Target.getZ5()));

        double totalAccuracy = 0.0;
        for (Double accuracy : zoneAccuracies) {
            totalAccuracy += accuracy;
        }

        return totalAccuracy / zoneAccuracies.size();
    }

    // Process and merge IA and val CMC7 data
    public CMC7 processAndMerge(String IACMC7, String valCMC7) {
        CMC7 extractedCMC7 = extractZones(IACMC7, valCMC7);
        CMC7 valCMC7Obj = new CMC7(valCMC7);

        System.out.println("\n=== Extracted CMC7 ===");
        printCMC7Table(extractedCMC7);
        double extractedAccuracy = calculateAccuracy(extractedCMC7, valCMC7Obj);
        System.out.println("Extracted Accuracy: " + String.format("%.2f", extractedAccuracy) + "%");

        // Choose merge strategy based on extracted accuracy
        CMC7 finalMergedCMC7;
        String mergeType;
        if (extractedAccuracy >= 50.0) {
            finalMergedCMC7 = fusion(extractedCMC7, valCMC7Obj);
            mergeType = "Full Merge";
        } else {
            finalMergedCMC7 = fusion(extractedCMC7, valCMC7Obj, (IAZone, valZone) -> {
                StringBuilder merged = new StringBuilder();
                int correctCount = 0;
                List<Integer> missingIndices = new ArrayList<>();
                for (int i = 0; i < IAZone.length(); i++) {
                    if (IAZone.charAt(i) != '?' && IAZone.charAt(i) == valZone.charAt(i)) {
                        correctCount++;
                        merged.append(IAZone.charAt(i));
                    } else if (IAZone.charAt(i) == '?') {
                        missingIndices.add(i);
                        merged.append('?');
                    } else {
                        merged.append('?');
                    }
                }
                int zoneLength = IAZone.length();
                int targetCorrect = (int) Math.round(zoneLength * 0.5);
                int charsToMerge = Math.max(0, targetCorrect - correctCount);
                if (charsToMerge > 0 && !missingIndices.isEmpty()) {
                    Collections.shuffle(missingIndices, new Random());
                    for (int k = 0; k < Math.min(charsToMerge, missingIndices.size()); k++) {
                        int idx = missingIndices.get(k);
                        merged.setCharAt(idx, valZone.charAt(idx));
                    }
                }
                return merged.toString();
            });
            mergeType = "Custom Merge";
        }
        System.out.println("\n=== Final " + mergeType + " CMC7 ===");
        printCMC7Table(finalMergedCMC7);
        double finalMergeAccuracy = calculateAccuracy(finalMergedCMC7, valCMC7Obj);
        System.out.println("Final Merge Accuracy: " + String.format("%.2f", finalMergeAccuracy) + "%");

        System.out.println("\nComparing extracted CMC7 and val CMC7: " + extractedCMC7.equals(valCMC7Obj));
        System.out.println("Final Merged CMC7 as string: " + finalMergedCMC7);

        return finalMergedCMC7;
    }

    // Main method for testing
    public static void main(String[] args) {
        CMC7ExtractorService service = CMC7ExtractorService.getInstance();

        // Get a random CMC7 pair
        Map<String, String> cmc7Pair = getRandomCMC7Pair();
        String valCMC7 = cmc7Pair.get("val");
        String IACMC7 = cmc7Pair.get("IA");

        System.out.println("Selected val CMC7\t: " + valCMC7);
        System.out.println("Selected IA CMC7\t: " + IACMC7);

        // Use the processAndMerge method
        CMC7 finalMergedCMC7 = service.processAndMerge(IACMC7, valCMC7);
    }

    // Method to get random CMC7 pair for testing
    public static Map<String, String> getRandomCMC7Pair() {
        String[][] cmc7Dataset = {
            //{"000;9264191;021780;0000048030014696;24", "00<9?641?1>021780>0000048030014696>24:"},
            //{"000;6444357;021780;0000047030758996;69", "<00<6444357>021780>0000047030758996>69:"},
            {"000;9438936;021780;0000238030035231;84", "<00<??38936>0217?0>0000?380?0035231;34:"},
            //{"000;9531895;021780;0000027030435244;67", "<00<9531895>021780>0000027030435244>67?"},
            //{"000;9820569;021780;0000051030090142;69", "<00<9820569:021780>00000510?0090142?69?"},
            {"000;9424775;021780;0000184027003781;81", "<00<9424775>021780>0000184027003781>31:"},
            //{"000;9751179;021780;0000191030019184;07", "00<9751179>0217?0>00001910 0019184>07:"},
            //{"000;8997892;021450;0000112030060841;55", "<00<8997892>021450>0000112030060841>55:"},
            {"000;9776840;021041;0000168030091512;85", "<00<9?76840>021041>0000168030091512>35:"},
            //{"000;9751178;021780;0000191030019184;07", "<00;9761178>021780>00001910?0019184>07:"},
            //{"000;9686943;021400;0000082030253433;52", "<00<9686943>021400>0000082030253433>52:"},
            //{"000;9882862;021780;0000027030052011;25", "<00<9882862>021780>00000?7030052011>25:"},
            //{"000;8953166;021780;0000027030196548;04", "<00<8953166>021780>0000027030196548>04:"},
            {"000;9885075;021780;0000196030096483;12", "<00<9885075>021780>0000196030096483>1??"},
            //{"000;9472725;021780;0000027030102821;79", "<00<9472725>021780>000002703010?821>79"},
            //{"000;9735730;021780;0000059030073066;41", "???????<????????????6???????????????????????0????"},
            //{"000;8733796;021780;0000176027119362;06", "176<8733796?02178 > ?? 1 6 119362>06:"},
            //{"000;6891630;021780;0000027030494327;37", "027<6891630>021780>0000027030494327>37:"}
        };

        Random random = new Random();
        int index = random.nextInt(cmc7Dataset.length);

        Map<String, String> cmc7Pair = new HashMap<>();
        cmc7Pair.put("val", cmc7Dataset[index][0]);
        cmc7Pair.put("IA", cmc7Dataset[index][1]);

        return cmc7Pair;
    }

    public static String intelligentFusion(String s1, String s2, String s3) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        if (s3 == null) s3 = "";

        int maxLength = Math.max(s1.length(), Math.max(s2.length(), s3.length()));
        if (maxLength == 0) return "";

        char[] c1 = String.format("%-" + maxLength + "s", s1).replace(' ', '?').toCharArray();
        char[] c2 = String.format("%-" + maxLength + "s", s2).replace(' ', '?').toCharArray();
        char[] c3 = String.format("%-" + maxLength + "s", s3).replace(' ', '?').toCharArray();

        List<String> lastAttemptHolder = new ArrayList<>();
        String validFusion = findValidFusionRecursive(c1, c2, c3, 0, new StringBuilder(), lastAttemptHolder);

        if (validFusion != null) {
            return validFusion;
        }
        if (!lastAttemptHolder.isEmpty()) {
            return lastAttemptHolder.get(0);
        }
        return s1;
    }


    private static String findValidFusionRecursive(char[] c1, char[] c2, char[] c3, int index, StringBuilder currentFusion, List<String> lastAttemptHolder) {
        if (index == c1.length) {
            String finalFusion = currentFusion.toString();
            if (lastAttemptHolder.isEmpty()) lastAttemptHolder.add(finalFusion);
            else lastAttemptHolder.set(0, finalFusion);

            String ribToValidate = extractRibForValidation(finalFusion);
            return isCorrectRIBKeyFromCMC7(ribToValidate) ? finalFusion : null;
        }

        Set<Character> candidates = new LinkedHashSet<>();
        candidates.add(c1[index]);
        candidates.add(c2[index]);
        candidates.add(c3[index]);

        for (char candidate : candidates) {
            if (candidate == '?') continue;

            currentFusion.append(candidate);
            String result = findValidFusionRecursive(c1, c2, c3, index + 1, currentFusion, lastAttemptHolder);
            if (result != null) {
                return result;
            }
            currentFusion.deleteCharAt(currentFusion.length() - 1);
        }
        return null;
    }

     public static String extractRibForValidation(String rectifiedCmc7) {
        if (rectifiedCmc7 == null) {
            return "";
        }
        String cleaned = rectifiedCmc7.replace(SEPARATOR, "").replace(":", "");
        if (cleaned.length() >= 24) {
            return cleaned.substring(cleaned.length() - 24);
        }
        return cleaned;
    }

    // SIMPLE FUSION
     public static String simpleFusion(String primary, String secondary) {
    	    if (primary == null && secondary == null) return "";
    	    if (primary == null) primary = "";
    	    if (secondary == null) secondary = "";

    	    int maxLength = Math.max(primary.length(), secondary.length());
    	    if (maxLength == 0) return "";

    	    char[] primaryChars = String.format("%-" + maxLength + "s", primary).replace(' ', '?').toCharArray();
    	    char[] secondaryChars = String.format("%-" + maxLength + "s", secondary).replace(' ', '?').toCharArray();

    	    StringBuilder fused = new StringBuilder();
    	    for (int i = 0; i < maxLength; i++) {
    	        if (primaryChars[i] != '?') {
    	            fused.append(primaryChars[i]);
    	        } else {
    	            fused.append(secondaryChars[i]);
    	        }
    	    }
    	    return fused.toString();
    	}


    public static boolean isCorrectRIBKeyFromCMC7(String cmc7) {
        if (cmc7 == null || cmc7.length() < 38) return false;
        String[] parts = cmc7.split(";");
        if (parts.length < 5) return false;
        String rib = parts[2] + parts[3] + parts[4];
        if (rib.length() != 24) return false;
        try {
            long rib1 = Long.parseLong(rib.substring(0, 12));
            long rib2 = Long.parseLong(rib.substring(12, 22) + "00");
            long res1 = rib1 % 97;
            String sRes1 = String.format("%02d", res1);
            String sRib2 = String.format("%012d", rib2);
            long rib3 = Long.parseLong(sRes1 + sRib2);
            long res2 = rib3 % 97;
            String sClec = String.format("%02d", 97 - res2);
            return rib.substring(22, 24).equals(sClec);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Trouve la meilleure chaîne candidate (celle avec le moins de '?').
     */
    public static String findBestCandidate(List<String> candidates) {
        String bestCandidate = "";
        int minQuestionMarks = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) continue;

            int currentQuestionMarks = 0;
            for (char c : candidate.toCharArray()) {
                if (c == '?') {
                    currentQuestionMarks++;
                }
            }

            if (currentQuestionMarks < minQuestionMarks) {
                minQuestionMarks = currentQuestionMarks;
                bestCandidate = candidate;
            }
        }
        //logger.info("Meilleur candidat choisi (avec {} '?') : {}", minQuestionMarks, bestCandidate);
        return bestCandidate;
    }


}