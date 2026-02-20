package com.charisad.cardutil;

public class Card {

    /**
     * Calculate Luhn 10 check digit.
     * @param cardNumber Number excluding check digit.
     * @return Check digit.
     */
    public static String calculateCheckDigit(String cardNumber) {
        // Python: digits = [int(digit) for digit in card_number if digit.isdigit()]
        // total = sum([sum(divmod(multiplier * digit, 10)) for digit, multiplier in zip(digits[::-1], cycle([2, 1]))])
        // return str((total * 9) % 10)
        
        int sum = 0;
        boolean alternate = true;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return String.valueOf((sum * 9) % 10);
    }
    
    public static boolean validateCheckDigit(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 2) return false;
        String number = cardNumber.substring(0, cardNumber.length() - 1);
        String checkDigit = cardNumber.substring(cardNumber.length() - 1);
        return calculateCheckDigit(number).equals(checkDigit);
    }
    
    public static String addCheckDigit(String cardNumber) {
        return cardNumber + calculateCheckDigit(cardNumber);
    }
    
    public static String mask(String cardNumber) {
        return mask(cardNumber, '*');
    }

    public static String mask(String cardNumber, char maskChar) {
        if (cardNumber == null || cardNumber.length() <= 10) return cardNumber; 
        // First 6, last 4
        StringBuilder sb = new StringBuilder();
        sb.append(cardNumber.substring(0, 6));
        for (int i = 0; i < cardNumber.length() - 10; i++) {
            sb.append(maskChar);
        }
        sb.append(cardNumber.substring(cardNumber.length() - 4));
        return sb.toString();
    }
}
