package demo.vencs;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*!
 * This plugin is based on the following projects.
 *
 * nzh v1.0.4
 * @source http://cnwhy.github.io/nzh
 * @license License BSD-2-Clause
 *
 * js-written-number
 * @source https://github.com/yamadapc/js-written-number
 * @license MIT License
 */
public class Number2Word {
    private static final Pattern REG_NUMBER = Pattern.compile("^([+-])?0*(\\d+)(\\.(\\d+))?$");
    private static final Pattern REG_E = Pattern.compile("^([+-])?0*(\\d+)(\\.(\\d+))?e(([+-])?(\\d+))$", Pattern.CASE_INSENSITIVE);

    public static String execute(String number, String lang, Map<String, Object> options) {
        HashMap<String, Object> defaultSetting = new HashMap<>();

        if ("cht".equals(lang)) {
            defaultSetting.put("ww", true);
            ChinesePack chtPack;
            if (options.containsKey("smallCase") && (boolean) options.get("smallCase")) {
                chtPack = chtSmall;
                defaultSetting.put("tenMin", true);
            } else {
                chtPack = chtBig;
            }
            return translateToChinese(number, mergeSetting(options, defaultSetting), chtPack);
        } else if ("chs".equals(lang)) {
            defaultSetting.put("ww", true);
            ChinesePack chsPack;
            if (options.containsKey("smallCase") && (boolean) options.get("smallCase")) {
                chsPack = chsSmall;
                defaultSetting.put("tenMin", true);
            } else {
                chsPack = chsBig;
            }
            return translateToChinese(number, mergeSetting(options, defaultSetting), chsPack);
        } else {
            defaultSetting.put("noAnd", false);
            return translate(number, lang, mergeSetting(options, defaultSetting));
        }
    }

    /**
     * @param numberString with scientific notation
     * @return decimal number string
     */
    private static String eToDecimal(String numberString) {
        Matcher matcher = REG_E.matcher(numberString + "");
        if (!matcher.matches()) return numberString + "";
        String intForE = matcher.group(2);
        String decimalForE = Optional.ofNullable(matcher.group(4)).orElse("");
        int e = Optional.ofNullable(matcher.group(5)).map(Integer::parseInt).orElse(0);
        if (e > 0) {
            String _intForE = decimalForE.substring(0, (e < decimalForE.length() ? e : decimalForE.length()));
            if (_intForE.length() < e) {
                _intForE = _intForE + String.join("", Collections.nCopies(e - _intForE.length(), "0"));
            }
            if (e < decimalForE.length()) {
                decimalForE = decimalForE.substring(e);
            } else {
                decimalForE = "";
            }
            intForE += _intForE;
        } else {
            int intForELength = intForE.length();
            e = -e;
            int s_start = intForELength - e;
            s_start = s_start < 0 ? 0 : s_start;
            int targetIndex = s_start + e;
            String _decimalForE = intForE.substring(s_start, (targetIndex < intForELength ? targetIndex : intForELength));
            if (_decimalForE.length() < e) {
                _decimalForE = _decimalForE + String.join("", Collections.nCopies(e - _decimalForE.length(), "0"));
            }
            intForE = intForE.substring(0, s_start);
            decimalForE = _decimalForE + decimalForE;
        }
        if (intForE.length() == 0) intForE = "0";
        return ("-".equals(matcher.group(1)) ? "-" : "") + intForE + (!decimalForE.isEmpty() ? "." + decimalForE : "");
    }

    private static synchronized Optional<NumberResult> getNumberResult(String numberString) {
        Matcher matcher = REG_NUMBER.matcher(numberString);
        if (!matcher.matches()) {
            matcher = REG_NUMBER.matcher(eToDecimal(numberString));
        }
        if (!matcher.matches()) {
            return Optional.empty();
        } else {
            return Optional.of(new NumberResult(
                    matcher.group(2), matcher.group(4), "-".equals(matcher.group(1))));
        }
    }

    private static class NumberResult {
        String intPart;
        String decimalPart;
        boolean isNegative;

        NumberResult(String intPart, String decimalPart, boolean isNegative) {
            this.intPart = intPart;
            this.decimalPart = decimalPart;
            this.isNegative = isNegative;
        }

        String getIntPart() {
            return intPart;
        }

        String getDecimalPart() {
            return decimalPart;
        }

        public boolean isNegative() {
            return isNegative;
        }
    }

    private static Map<String, Object> mergeSetting(Map<String, Object> target, Map<String, Object> defaultSetting) {
        Map<String, Object> merged = new HashMap<>();

        merged.putAll(target);
        defaultSetting.keySet().parallelStream()
                .filter(defaultKey -> !merged.containsKey(defaultKey))
                .forEach(defaultKey -> merged.put(defaultKey, defaultSetting.get(defaultKey)));

        return merged;
    }

    private static String clearZero(String str, String zero, String type) {
        if (str == null || str.isEmpty()) return "";
        String reg0 = "*.?+$^[](){}|\\/".contains(zero) ? "\\\\" + zero : zero;
        if (type.isEmpty() || "$".equals(type)) {
            str = str.replaceAll(reg0 + "+$", "");
        }
        if (type.isEmpty()) {
            str = str.replaceAll(reg0 + "{2}", zero);
        }
        return str;
    }

    private static String translateToChinese(String number, Map<String, Object> options, ChinesePack chinesePack) {
        Optional<NumberResult> OptionalResult = getNumberResult(number);
        if (!OptionalResult.isPresent()) {
            return number;
        }
        NumberResult result = OptionalResult.get();

        String _int = result.getIntPart();
        String _decimal = result.getDecimalPart();

        String intPart = encodeChineseInteger(_int, options, chinesePack);
        StringBuilder decimalPart = new StringBuilder();

        if (_decimal != null && !_decimal.isEmpty()) {
            _decimal = clearZero(_decimal, "0", "$"); // remove trailing zero
            for (int x = 0; x < _decimal.length(); x++) {
                char xChar = _decimal.charAt(x);
                decimalPart.append(chinesePack.chars.charAt(Integer.parseInt(xChar + "")));
            }
            if (decimalPart.length() > 0) {
                decimalPart.insert(0, chinesePack.dotChar);
            }
        }

        // 超級大數的萬進制, e.g.萬萬億為兆
        if (options.containsKey("ww") && (boolean) options.get("ww") && chinesePack.unit.length() > 5) {
            char dw_w = chinesePack.unit.charAt(4), dw_y = chinesePack.unit.charAt(5);
            int lastY = intPart.lastIndexOf(dw_y);
            if (lastY != -1) {
                intPart = intPart.substring(0, lastY).replaceAll(dw_y + "", dw_w + "" + dw_w) + intPart.substring(lastY);
            }
        }
        return (result.isNegative ? chinesePack.negativeChar : "") + intPart + decimalPart;
    }

    private static String encodeChineseInteger(String intPart, Map<String, Object> options, ChinesePack chinesePack) {
        intPart = getNumberResult(intPart).map(NumberResult::getIntPart).orElse(intPart);
        StringBuilder encoded = new StringBuilder();
        boolean isTenMin = options.containsKey("tenMin") && (boolean) options.get("tenMin");
        char n0 = chinesePack.chars.charAt(0);
        int length = intPart.length();
        if (length == 1) {
            return chinesePack.chars.charAt(Integer.parseInt(intPart)) + "";
        }
        if (length <= 4) {
            for (int i = 0; i < length; i++) {
                int n = length - i - 1;
                int num = Integer.parseInt(intPart.charAt(i) + "");
                encoded.append((isTenMin && length == 2 && i == 0 && num == 1) ? "" : chinesePack.chars.charAt(num));
                encoded.append(num != 0 && n > 0 ? chinesePack.unit.charAt(n) : "");
            }
        } else {
            int d = length / 4, y = length % 4;
            while (y == 0 || chinesePack.unit.length() <= 3 + d) {
                y += 4;
                d--;
            }

            String _maxLeft = intPart.substring(0, y);
            String _other = intPart.substring(y);
            encoded = new StringBuilder(encodeChineseInteger(_maxLeft, options, chinesePack) + chinesePack.unit.charAt(3 + d)
                    + (_other.charAt(0) == '0' ? n0 : "")
                    + encodeChineseInteger(_other, options, chinesePack));
        }
        encoded = new StringBuilder(clearZero(encoded.toString(), n0 + "", ""));
        return encoded.toString();
    }

    private static class ChinesePack {
        String chars;
        String unit;
        String negativeChar;
        String dotChar;

        ChinesePack(String chars, String unit, String negativeChar, String dotChar) {
            this.chars = chars;
            this.unit = unit;
            this.negativeChar = negativeChar;
            this.dotChar = dotChar;
        }
    }

    private static ChinesePack chtSmall = new ChinesePack(
            "零一二三四五六七八九",
            "個十百千萬億",
            "負",
            "點"
    );

    private static ChinesePack chtBig = new ChinesePack(
            "零壹貳參肆伍陸柒捌玖",
            "個拾佰仟萬億",
            "負",
            "點"
    );

    private static ChinesePack chsSmall = new ChinesePack(
            "零一二三四五六七八九",
            "个十百千万亿",
            "负",
            "点"
    );

    private static ChinesePack chsBig = new ChinesePack(
            "零壹贰叁肆伍陆柒捌玖",
            "个拾佰仟万亿",
            "负",
            "点"
    );

    private static String translate(String number, String lang, Map<String, Object> options) {
        Optional<NumberResult> OptionalResult = getNumberResult(number);
        if (!OptionalResult.isPresent()) {
            return number;
        }
        NumberResult result = OptionalResult.get();

        LanguagePack language;
        if (i18n.containsKey(lang)) {
            language = i18n.get(lang);
        } else {
            language = i18n.get("en");
        }

        String words;
        String integerPart = encodeInteger(result.getIntPart(), language, options);
        if (result.getDecimalPart() == null) {
            words = integerPart;
        } else {
            String decimalPart = result.getDecimalPart();
            StringBuilder sb = new StringBuilder();
            sb.append(language.dotWord);
            for (char singleChar : decimalPart.toCharArray()) {
                sb.append(" " + language.base.get(singleChar + ""));
            }
            words = integerPart + " " + sb.toString();
        }

        return result.isNegative ? language.negativeWord + " " + words : words;
    }

    private final static double[] shortScale = new double[]{100, 1000, 1000000, 1000000000, 1000000000000d,
            1000000000000000d, 1000000000000000000d, 1e+21, 1e+24, 1e+27, 1e+30, 1e+33, 9.999999999999999e+35,
            1.0000000000000001e+39, 9.999999999999999e+41, 1e+45, 1e+48};

    private final static double[] longScale = new double[]{100, 1000, 1000000, 1000000000000d, 1000000000000000000d,
            1e+24, 1e+30, 9.999999999999999e+35, 9.999999999999999e+41, 1e+48, 1e+54, 1e+60, 1e+66, 1e+72, 1e+78, 1e+84, 1e+90};

    private static String encodeInteger(String intPart, LanguagePack language, Map<String, Object> options) {
        final double[] scale = language.useLongScale ? longScale : shortScale;
        final List<LanguageUnit> units = language.units;
        final Map<String, String> baseCardinals = language.base;

        if (language.unitExceptions.containsKey(intPart)) {
            return language.unitExceptions.get(intPart);
        }
        if (baseCardinals.containsKey(intPart)) {
            return baseCardinals.get(intPart);
        }
        long n = Long.parseLong(intPart);
        if (n < 100) {
            return handleSmallerThan100(n, language, options);
        }

        long m = n % 100;
        List<String> ret = new ArrayList<>();
        if (m != 0) {
            if (options.containsKey("noAnd") && (boolean) options.get("noAnd")) {
                ret.add(encodeInteger(m + "", language, options));
            } else {
                ret.add(language.unitSeparator + encodeInteger(m + "", language, options));
            }
        }

        LanguageUnit unit;
        for (int i = 0, len = units.size(); i < len; i++) {
            Double r = Math.floor(n / scale[i]);
            double divideBy;

            if (i == len - 1) divideBy = 1000000;
            else divideBy = scale[i + 1] / scale[i];

            r %= divideBy;

            unit = units.get(i);

            if (r == 0) continue;

            String str;
            if (unit.isSimpleUnit()) {
                str = unit.getSimpleValue();
            } else {
                if (!unit.getPlural().isEmpty() && (!unit.isAvoidInNumberPlural() || m == 0)) {
                    str = unit.getPlural();
                } else {
                    str = unit.getSingular();
                }
            }

            final double compare = r;
            if (unit.getAvoidPrefixException() != null &&
                    IntStream.of(unit.getAvoidPrefixException()).anyMatch(x -> x == compare)) {
                ret.add(str);
                continue;
            }

            String exception = language.unitExceptions.get(r.longValue() + "");
            String number;
            if (exception != null && !exception.isEmpty()) {
                number = exception;
            } else {
                Map<String, Object> changed = new HashMap<>();
                changed.put("noAnd", true);
                number = encodeInteger(r.longValue() + "", language, mergeSetting(changed, options));
            }
            n -= r * scale[i];
            ret.add(number + " " + str);
        }

        Collections.reverse(ret);
        return String.join(" ", ret);
    }

    private static String handleSmallerThan100(long n, LanguagePack language, Map<String, Object> options) {
        long dec = new Double(Math.floor(n / 10) * 10).longValue();
        long unit = n - dec;

        Map<String, String> baseCardinals = language.base;

        if (unit != 0) {
            return baseCardinals.get(dec + "") + language.baseSeparator + encodeInteger(unit + "", language, options);
        }
        return baseCardinals.get(dec + "");
    }

    private static class LanguagePack {
        boolean useLongScale;
        String baseSeparator;
        String unitSeparator;
        String negativeWord;
        String dotWord;
        Map<String, String> base;
        List<LanguageUnit> units;
        Map<String, String> unitExceptions;

        LanguagePack(boolean useLongScale, String baseSeparator, String unitSeparator,
                     String negativeWord, String dotWord,
                     String[] baseKey, String[] baseValue,
                     LanguageUnit[] units, String[] unitExceptionsKey, String[] unitExceptionsValue) {
            this.useLongScale = useLongScale;
            this.baseSeparator = baseSeparator;
            this.unitSeparator = unitSeparator;
            this.negativeWord = negativeWord;
            this.dotWord = dotWord;
            this.base = IntStream.range(0, baseKey.length).boxed()
                    .collect(Collectors.toMap(i -> baseKey[i], i -> baseValue[i]));
            this.units = Arrays.stream(units).collect(Collectors.toList());
            this.unitExceptions = IntStream.range(0, unitExceptionsKey.length).boxed()
                    .collect(Collectors.toMap(i -> unitExceptionsKey[i], i -> unitExceptionsValue[i]));
        }
    }

    private static class LanguageUnit {
        private boolean isSimpleUnit;
        private String singular;
        private String plural;
        private boolean avoidInNumberPlural = false;
        private int[] avoidPrefixException;

        LanguageUnit(String value) {
            this.singular = value;
            this.isSimpleUnit = true;
        }

        LanguageUnit(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
            this.isSimpleUnit = false;
        }

        LanguageUnit(String singular, String plural, boolean avoidInNumberPlural, int[] avoidPrefixException) {
            this(singular, plural);
            this.avoidInNumberPlural = avoidInNumberPlural;
            this.avoidPrefixException = avoidPrefixException;
        }

        boolean isSimpleUnit() {
            return isSimpleUnit;
        }

        String getSimpleValue() {
            return singular;
        }

        String getSingular() {
            return singular;
        }

        String getPlural() {
            return plural;
        }

        boolean isAvoidInNumberPlural() {
            return avoidInNumberPlural;
        }

        int[] getAvoidPrefixException() {
            return avoidPrefixException;
        }
    }

    private final static LanguagePack enPack = new LanguagePack(false, "-", "and ",
            "negative", "point",
            new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "30", "40", "50", "60", "70", "80", "90"},
            new String[]{"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
                    "nineteen", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"},
            new LanguageUnit[]{new LanguageUnit("hundred"), new LanguageUnit("thousand"), new LanguageUnit("million"),
                    new LanguageUnit("billion"), new LanguageUnit("trillion"), new LanguageUnit("quadrillion"),
                    new LanguageUnit("quintillion"), new LanguageUnit("sextillion"), new LanguageUnit("septillion"),
                    new LanguageUnit("octillion"), new LanguageUnit("nonillion"), new LanguageUnit("decillion"),
                    new LanguageUnit("undecillion"), new LanguageUnit("duodecillion"), new LanguageUnit("tredecillion"),
                    new LanguageUnit("quattuordecillion"), new LanguageUnit("quindecillion")},
            new String[]{}, new String[]{});

    private final static LanguagePack frPack = new LanguagePack(false, "-", "",
            "moins", "point",
            new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "30", "40", "50", "60", "70", "80", "90"},
            new String[]{"zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze",
                    "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf", "vingt",
                    "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"},
            new LanguageUnit[]{new LanguageUnit("cent", "cents", true, new int[]{1}),
                    new LanguageUnit("mille", "", false, new int[]{1}),
                    new LanguageUnit("million", "millions"), new LanguageUnit("milliard", "milliards"),
                    new LanguageUnit("billion", "billions"), new LanguageUnit("billiard", "billiards"),
                    new LanguageUnit("trillion", "trillions"), new LanguageUnit("trilliard", "trilliards"),
                    new LanguageUnit("quadrillion", "quadrillions"), new LanguageUnit("quadrilliard", "quadrilliards"),
                    new LanguageUnit("quintillion", "quintillions"), new LanguageUnit("quintilliard", "quintilliards"),
                    new LanguageUnit("sextillion", "sextillions"), new LanguageUnit("sextilliard", "sextilliards"),
                    new LanguageUnit("septillion", "septillions"), new LanguageUnit("septilliard", "septilliards"),
                    new LanguageUnit("octillion", "octillions")
            },
            new String[]{"21", "31", "41", "51", "61", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "91",
                    "92", "93", "94", "95", "96", "97", "98", "99"},
            new String[]{"vingt et un", "trente et un", "quarante et un", "cinquante et un", "soixante et un",
                    "soixante et onze", "soixante-douze", "soixante-treize", "soixante-quatorze", "soixante-quinze",
                    "soixante-seize", "soixante-dix-sept", "soixante-dix-huit", "soixante-dix-neuf", "quatre-vingts",
                    "quatre-vingt-onze", "quatre-vingt-douze", "quatre-vingt-treize", "quatre-vingt-quatorze",
                    "quatre-vingt-quinze", "quatre-vingt-seize", "quatre-vingt-dix-sept", "quatre-vingt-dix-huit",
                    "quatre-vingt-dix-neuf"});

    private final static Map<String, LanguagePack> i18n = new HashMap<>();

    static {
        i18n.put("en", enPack);
        i18n.put("fr", frPack);
    }
}