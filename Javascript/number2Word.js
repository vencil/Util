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
let Number2Word = (function () {
    'use strict';

    const REG_NUMBER = /^([+-])?0*(\d+)(\.(\d+))?$/;
    const REG_E = /^([+-])?0*(\d+)(\.(\d+))?e(([+-])?(\d+))$/i;

    /**
     * @param num with scientific notation
     * @return {string} decimal number string
     */
    function eToDecimal(num) {
        let result = REG_E.exec(num.toString());
        if (!result) return num;
        let intForE = result[2],
            decimalForE = result[4] || "",
            e = result[5] ? +result[5] : 0;
        if (e > 0) {
            let _intForE = decimalForE.substr(0, e);
            _intForE = _intForE.length < e ? _intForE + new Array(e - _intForE.length + 1).join("0") : _intForE;
            decimalForE = decimalForE.substr(e);
            intForE += _intForE;
        } else {
            e = -e;
            let s_start = intForE.length - e;
            s_start = s_start < 0 ? 0 : s_start;
            let _decimalForE = intForE.substr(s_start, e);
            _decimalForE = _decimalForE.length < e ? new Array(e - _decimalForE.length + 1).join("0") + _decimalForE : _decimalForE;
            intForE = intForE.substring(0, s_start);
            decimalForE = _decimalForE + decimalForE;
        }
        if (intForE === "") intForE = "0";
        return (result[1] === "-" ? "-" : "") + intForE + (decimalForE ? "." + decimalForE : "");
    }

    function getNumberResult(num) {
        let result = REG_NUMBER.exec(num.toString());
        if (!result && REG_E.test(num.toString())) {
            result = REG_NUMBER.exec(eToDecimal(num.toString()));
        }
        if (result) {
            return {
                intPart: result[2],
                decimalPart: result[4],
                isNegative: result[1] === "-"
            }
        }
        return null;
    }

    /**
     * Merges a set of default keys with a target object
     *
     * @param {Object} [target] The object to extend
     * @param {Object} defaultSetting The object to default to
     * @return {Object} extendedTarget
     */
    function mergeSetting(target, defaultSetting) {
        if (target === null) target = {};
        const ret = {};
        const keys = Object.keys(defaultSetting);
        for (let i = 0, len = keys.length; i < len; i++) {
            let key = keys[i];
            if (target.hasOwnProperty(key)) {
                ret[key] = target[key]
            } else if (defaultSetting.hasOwnProperty(key)) {
                ret[key] = defaultSetting[key];
            }
        }
        return ret;
    }

    function clearZero(str, zero, type) {
        if (str === null) return "";
        let reg0 = "*.?+$^[](){}|\\/".includes(zero) ? "\\" + zero : zero;
        let arg_e = new RegExp(reg0 + "+$"),
            arg_d = new RegExp(reg0 + "{2}", "g");
        str = str.toString();
        if (!type || type === "$") {
            str = str.replace(arg_e, "");
        }
        if (!type) {
            str = str.replace(arg_d, zero);
        }
        return str;
    }

    function translateToChinese(num, options, chinesePack) {
        let result = getNumberResult(num);
        if (!result) {
            return num;
        }
        let _int = result.intPart;
        let _decimal = result.decimalPart;

        let intPart = encodeChineseInteger(_int, options, chinesePack);
        let decimalPart = "";

        if (_decimal) {
            _decimal = clearZero(_decimal, "0", "$"); // remove trailing zero
            for (let x = 0; x < _decimal.length; x++) {
                decimalPart += chinesePack.chars.charAt(+_decimal.charAt(x));
            }
            if (decimalPart.length) {
                decimalPart = chinesePack.dotChar + decimalPart;
            }
        }

        // 超級大數的萬進制, e.g.萬萬億為兆
        if (options.ww && chinesePack.unit.length > 5) {
            const dw_w = chinesePack.unit.charAt(4), dw_y = chinesePack.unit.charAt(5);
            let lastY = intPart.lastIndexOf(dw_y);
            if (lastY !== -1) {
                intPart = intPart.substring(0, lastY).replace(new RegExp(dw_y, 'g'), dw_w + dw_w) + intPart.substring(lastY);
            }
        }
        return (result.isNegative ? chinesePack.negativeChar : "") + intPart + decimalPart;
    }

    function encodeChineseInteger(intPart, options, chinesePack) {
        const result = getNumberResult(intPart);
        if (result) {
            intPart = result.intPart;
        }
        let encoded = "";
        const isTenMin = options.tenMin, length = intPart.length;
        const n0 = chinesePack.chars.charAt(0);
        if (length === 1)
            return chinesePack.chars.charAt(+intPart);
        if (length <= 4) {
            let i = 0, n = length;
            for (; n--;) {
                const num = parseInt(intPart.charAt(i));
                encoded += (isTenMin && length === 2 && i === 0 && num === 1) ? "" : chinesePack.chars.charAt(num);
                encoded += (num && n ? chinesePack.unit.charAt(n) : '');
                i++;
            }
        } else {
            let d = parseInt(length / 4),
                y = length % 4;
            while (y === 0 || chinesePack.unit.length <= 3 + d) {
                y += 4;
                d--;
            }

            const _maxLeft = intPart.substr(0, y),
                _other = intPart.substr(y);

            encoded = encodeChineseInteger(_maxLeft, options, chinesePack) + chinesePack.unit.charAt(3 + d)
                + (_other.charAt(0) === '0' ? n0 : '')
                + encodeChineseInteger(_other, options, chinesePack);
        }
        encoded = clearZero(encoded, n0, "");
        return encoded;
    }

    const chtSmall = {
        chars: '零一二三四五六七八九',
        unit: '個十百千萬億',
        negativeChar: '負',
        dotChar: '點'
    };

    const chtBig = {
        chars: '零壹貳參肆伍陸柒捌玖',
        unit: '個拾佰仟萬億',
        negativeChar: '負',
        dotChar: '點'
    };

    const chsSmall = {
        chars: '零一二三四五六七八九',
        unit: '个十百千万亿',
        negativeChar: '负',
        dotChar: '点'
    };

    const chsBig = {
        chars: '零壹贰叁肆伍陆柒捌玖',
        unit: '个拾佰仟万亿',
        negativeChar: '负',
        dotChar: '点'
    };

    const i18n = {
        "en": {
            "useLongScale": false,
            "baseSeparator": "-",
            "unitSeparator": "and ",
            "negativeWord": "negative",
            "dotWord": "point",
            "base": {
                "0": "zero",
                "1": "one",
                "2": "two",
                "3": "three",
                "4": "four",
                "5": "five",
                "6": "six",
                "7": "seven",
                "8": "eight",
                "9": "nine",
                "10": "ten",
                "11": "eleven",
                "12": "twelve",
                "13": "thirteen",
                "14": "fourteen",
                "15": "fifteen",
                "16": "sixteen",
                "17": "seventeen",
                "18": "eighteen",
                "19": "nineteen",
                "20": "twenty",
                "30": "thirty",
                "40": "forty",
                "50": "fifty",
                "60": "sixty",
                "70": "seventy",
                "80": "eighty",
                "90": "ninety"
            },
            "units": [
                "hundred",
                "thousand",
                "million",
                "billion",
                "trillion",
                "quadrillion",
                "quintillion",
                "sextillion",
                "septillion",
                "octillion",
                "nonillion",
                "decillion",
                "undecillion",
                "duodecillion",
                "tredecillion",
                "quattuordecillion",
                "quindecillion"
            ],
            "unitExceptions": {}
        },
        "fr": {
            "useLongScale": false,
            "baseSeparator": "-",
            "unitSeparator": "",
            "negativeWord": "moins",
            "dotWord": "point",
            "base": {
                "0": "zéro",
                "1": "un",
                "2": "deux",
                "3": "trois",
                "4": "quatre",
                "5": "cinq",
                "6": "six",
                "7": "sept",
                "8": "huit",
                "9": "neuf",
                "10": "dix",
                "11": "onze",
                "12": "douze",
                "13": "treize",
                "14": "quatorze",
                "15": "quinze",
                "16": "seize",
                "17": "dix-sept",
                "18": "dix-huit",
                "19": "dix-neuf",
                "20": "vingt",
                "30": "trente",
                "40": "quarante",
                "50": "cinquante",
                "60": "soixante",
                "70": "soixante-dix",
                "80": "quatre-vingt",
                "90": "quatre-vingt-dix"
            },
            "units": [
                {
                    "singular": "cent",
                    "plural": "cents",
                    "avoidInNumberPlural": true,
                    "avoidPrefixException": [1]
                },
                {
                    "singular": "mille",
                    "avoidPrefixException": [1]
                },
                {
                    "singular": "million",
                    "plural": "millions"
                },
                {
                    "singular": "milliard",
                    "plural": "milliards"
                },
                {
                    "singular": "billion",
                    "plural": "billions"
                },
                {
                    "singular": "billiard",
                    "plural": "billiards"
                },
                {
                    "singular": "trillion",
                    "plural": "trillions"
                },
                {
                    "singular": "trilliard",
                    "plural": "trilliards"
                },
                {
                    "singular": "quadrillion",
                    "plural": "quadrillions"
                },
                {
                    "singular": "quadrilliard",
                    "plural": "quadrilliards"
                },
                {
                    "singular": "quintillion",
                    "plural": "quintillions"
                },
                {
                    "singular": "quintilliard",
                    "plural": "quintilliards"
                },
                {
                    "singular": "sextillion",
                    "plural": "sextillions"
                },
                {
                    "singular": "sextilliard",
                    "plural": "sextilliards"
                },
                {
                    "singular": "septillion",
                    "plural": "septillions"
                },
                {
                    "singular": "septilliard",
                    "plural": "septilliards"
                },
                {
                    "singular": "octillion",
                    "plural": "octillions"
                }
            ],
            "unitExceptions": {
                "21": "vingt et un",
                "31": "trente et un",
                "41": "quarante et un",
                "51": "cinquante et un",
                "61": "soixante et un",
                "71": "soixante et onze",
                "72": "soixante-douze",
                "73": "soixante-treize",
                "74": "soixante-quatorze",
                "75": "soixante-quinze",
                "76": "soixante-seize",
                "77": "soixante-dix-sept",
                "78": "soixante-dix-huit",
                "79": "soixante-dix-neuf",
                "80": "quatre-vingts",
                "91": "quatre-vingt-onze",
                "92": "quatre-vingt-douze",
                "93": "quatre-vingt-treize",
                "94": "quatre-vingt-quatorze",
                "95": "quatre-vingt-quinze",
                "96": "quatre-vingt-seize",
                "97": "quatre-vingt-dix-sept",
                "98": "quatre-vingt-dix-huit",
                "99": "quatre-vingt-dix-neuf"
            }
        }
    };

    const shortScale = [100, 1000, 1000000, 1000000000, 1000000000000, 1000000000000000, 1000000000000000000, 1e+21, 1e+24, 1e+27, 1e+30, 1e+33, 9.999999999999999e+35, 1.0000000000000001e+39, 9.999999999999999e+41, 1e+45, 1e+48];
    const longScale = [100, 1000, 1000000, 1000000000000, 1000000000000000000, 1e+24, 1e+30, 9.999999999999999e+35, 9.999999999999999e+41, 1e+48, 1e+54, 1e+60, 1e+66, 1e+72, 1e+78, 1e+84, 1e+90];

    function translate(num, lang, options) {
        let result = getNumberResult(num);
        if (!result) {
            return num;
        }

        let language;
        if (i18n.hasOwnProperty(lang)) {
            language = i18n[lang];
        } else {
            language = i18n.en;
        }

        let words;
        let integerPart = encodeInteger(result.intPart, language, options);
        if (!result.decimalPart) {
            words = integerPart;
        } else {
            let _decimal = result.decimalPart;
            let _encode = _decimal.split('').map(function (singleChar) {
                return language.base[singleChar];
            });
            words = integerPart + " " + language.dotWord + " " + _encode.join(" ");
        }

        return result.isNegative ? language.negativeWord + " " + words : words;
    }

    function encodeInteger(intPart, language, options) {
        const scale = language.useLongScale ? longScale : shortScale;
        const units = language.units;
        const baseCardinals = language.base;

        if (language.unitExceptions[intPart]) return language.unitExceptions[intPart];
        if (baseCardinals[intPart]) return baseCardinals[intPart];
        let n = parseInt(intPart);
        if (n < 100) {
            return handleSmallerThan100(n, language, options);
        }

        let m = n % 100;
        let ret = [];
        if (m) {
            if (options.noAnd) {
                ret.push(encodeInteger(m, language, options));
            } else {
                ret.push(language.unitSeparator + encodeInteger(m, language, options));
            }
        }

        let unit;
        for (let i = 0, len = units.length; i < len; i++) {
            let r = Math.floor(n / scale[i]);
            let divideBy;

            if (i === len - 1) divideBy = 1000000;
            else divideBy = scale[i + 1] / scale[i];

            r %= divideBy;

            unit = units[i];

            if (r === 0) continue;

            let str;
            if (typeof unit === "string") {
                str = unit;
            } else {
                if (unit.plural && (!unit.avoidInNumberPlural || !m)) {
                    str = unit.plural;
                } else {
                    str = unit.singular;
                }
            }

            let exception = language.unitExceptions[r];
            let number;
            if (exception) {
                number = exception;
            } else {
                number = encodeInteger(r, language, mergeSetting({noAnd: true}, options));
            }
            n -= r * scale[i];
            ret.push(number + " " + str);
        }

        return ret.reverse().join(" ");
    }

    function handleSmallerThan100(n, language, options) {
        let dec = Math.floor(n / 10) * 10;
        let unit = n - dec;

        let baseCardinals = language.base;

        if (unit !== 0) {
            return baseCardinals[dec] + language.baseSeparator + encodeInteger(unit, language, options);
        }
        return baseCardinals[dec];
    }

    return function execute(number, lang, options) {
        lang = lang || "en";
        options = options || {};

        let defaultSetting = {};

        if (lang === "cht") {
            defaultSetting['ww'] = true;
            let chtPack;
            if (options.smallCase) {
                chtPack = chtSmall;
                defaultSetting['tenMin'] = true;
            } else {
                chtPack = chtBig;
            }
            return translateToChinese(number, mergeSetting(options, defaultSetting), chtPack);
        } else if (lang === "chs") {
            defaultSetting['ww'] = true;
            let chsPack;
            if (options.smallCase) {
                chsPack = chsSmall;
                defaultSetting['tenMin'] = true;
            } else {
                chsPack = chsBig;
            }
            return translateToChinese(number, mergeSetting(options, defaultSetting), chsPack);
        } else {
            defaultSetting['noAnd'] = false;
            return translate(number, lang, mergeSetting(options, defaultSetting));
        }
    }
})();