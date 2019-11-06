package demo.vencs;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBCodeParser {
    private static HashMap<String, BBCodeTag> tagMap;
    private static Pattern urlPattern;
    private static Pattern colorNamePattern;
    private static Pattern colorCodePattern;
    private static Pattern colorRGBPattern;
    private static Pattern emailPattern;
    private static Pattern bbclPattern;
    private static Pattern parsedTagPattern;
    private static Pattern noParsedTagPattern;
    private static Pattern checkDepthsPattern;
    private static Pattern subDepthsComparePattern;
    private static Pattern subDepthsPattern;

    private BBCodeParser() {
        initTags();

        urlPattern = Pattern.compile("^(?:https?):(?:\\/{1,3}|\\\\{1})[-a-zA-Z0-9:;,@#!%&()~_?\\+=\\/\\\\\\.]*$");
        colorNamePattern = Pattern.compile("^(?:aliceblue|antiquewhite|aqua|aquamarine|azure|beige|bisque|black|blanchedalmond|blue|blueviolet|brown|burlywood|cadetblue|chartreuse|chocolate|coral|cornflowerblue|cornsilk|crimson|cyan|darkblue|darkcyan|darkgoldenrod|darkgray|darkgreen|darkkhaki|darkmagenta|darkolivegreen|darkorange|darkorchid|darkred|darksalmon|darkseagreen|darkslateblue|darkslategray|darkturquoise|darkviolet|deeppink|deepskyblue|dimgray|dodgerblue|firebrick|floralwhite|forestgreen|fuchsia|gainsboro|ghostwhite|gold|goldenrod|gray|green|greenyellow|honeydew|hotpink|indianred|indigo|ivory|khaki|lavender|lavenderblush|lawngreen|lemonchiffon|lightblue|lightcoral|lightcyan|lightgoldenrodyellow|lightgray|lightgreen|lightpink|lightsalmon|lightseagreen|lightskyblue|lightslategray|lightsteelblue|lightyellow|lime|limegreen|linen|magenta|maroon|mediumaquamarine|mediumblue|mediumorchid|mediumpurple|mediumseagreen|mediumslateblue|mediumspringgreen|mediumturquoise|mediumvioletred|midnightblue|mintcream|mistyrose|moccasin|navajowhite|navy|oldlace|olive|olivedrab|orange|orangered|orchid|palegoldenrod|palegreen|paleturquoise|palevioletred|papayawhip|peachpuff|peru|pink|plum|powderblue|purple|red|rosybrown|royalblue|saddlebrown|salmon|sandybrown|seagreen|seashell|sienna|silver|skyblue|slateblue|slategray|snow|springgreen|steelblue|tan|teal|thistle|tomato|turquoise|violet|wheat|white|whitesmoke|yellow|yellowgreen)$");
        colorCodePattern = Pattern.compile("^#?[a-fA-F0-9]{6}$");
        colorRGBPattern = Pattern.compile("^rgb?\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*(\\d+))?\\)$");
        emailPattern = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

        StringBuilder parsedTagList = new StringBuilder();
        StringBuilder noParsedTagList = new StringBuilder();
        for (BBCodeTag bbCodeTag : tagMap.values()) {
            String tagName = bbCodeTag.getTagName();
            if (tagName.equals("*")) {
                tagName = "\\" + tagName;
            }
            parsedTagList.append(tagName + "|");

            if (bbCodeTag.isNoParse()) {
                noParsedTagList.append(tagName + "|");
            }
        }
        if (parsedTagList.length() > 0) {
            parsedTagList.setLength(parsedTagList.length() - 1);
        }
        if (noParsedTagList.length() > 0) {
            noParsedTagList.setLength(noParsedTagList.length() - 1);
        }

        bbclPattern = Pattern.compile("<bbcl=([0-9]+) (" + parsedTagList + ")([ =][^>]*?)?>((?:.|[\\r\\n])*?)<bbcl=\\1 \\/\\2>");
        parsedTagPattern = Pattern.compile("\\[(" + parsedTagList + ")([ =][^\\]]*?)?\\]([^\\[]*?)\\[\\/\\1\\]");
        noParsedTagPattern = Pattern.compile("\\[(" + noParsedTagList + ")([ =][^\\]]*?)?\\]([\\s\\S]*?)\\[\\/\\1\\]");

        checkDepthsPattern = Pattern.compile("\\<([^\\>][^\\>]*?)\\>");
        subDepthsComparePattern = Pattern.compile("^bbcl=([0-9]+) ");
        subDepthsPattern = Pattern.compile("^(bbcl=)([0-9]+)");
    }

    private static class LazyHolder {
        private static final BBCodeParser instance = new BBCodeParser();
    }

    public static BBCodeParser getInstance() {
        return LazyHolder.instance;
    }

    private void initTags() {
        tagMap = new HashMap<>();

        addTagToMap("b");
        addTagToMap("i");
        addTagToMap("u");
        addTagToMap("s");
        addTagToMap("sub");
        addTagToMap("sup");

        addTagToMap("h2");
        addTagToMap("h3");

        BBCodeTag codeTag = new BBCodeTag("code") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<pre>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</pre>";
            }
        };
        codeTag.setNoParse(true);
        addTagToMap(codeTag);

        addTagToMap("table");
        addTagToMap("tbody");
        addTagToMap("tfoot");
        addTagToMap("thead");
        addTagToMap("th");
        addTagToMap("tr");
        addTagToMap("td");

        BBCodeTag olTag = new BBCodeTag("ol") {
            @Override
            public String getOpenTag(String param, String content) {
                if (param.isEmpty()) return "<ol>";
                return "<ol style=\"list-style-type:" + param + "\">";
            }
        };
        addTagToMap(olTag);

        BBCodeTag ulTag = new BBCodeTag("ul") {
            @Override
            public String getOpenTag(String param, String content) {
                if (param.isEmpty()) return "<ul>";
                return "<ul style=\"list-style-type:" + param + "\">";
            }
        };
        addTagToMap(ulTag);

        BBCodeTag listTag = new BBCodeTag("list") {
            @Override
            public String getOpenTag(String param, String content) {
                if (param.isEmpty()) return "<ul>";
                return "<ul style=\"list-style-type:" + param + "\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</ul>";
            }
        };
        addTagToMap(listTag);

        addTagToMap("li");

        BBCodeTag colorTag = new BBCodeTag("color") {
            @Override
            public String getOpenTag(String param, String content) {
                String colorCode = param.toLowerCase();
                colorCode = getCorrectColorCode(colorCode);
                return "<span style=\"color:" + colorCode + "\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</span>";
            }
        };
        addTagToMap(colorTag);

        BBCodeTag backgroundColorTag = new BBCodeTag("bgcolor") {
            @Override
            public String getOpenTag(String param, String content) {
                String colorCode = param.toLowerCase();
                colorCode = getCorrectColorCode(colorCode);
                return "<span style=\"background-color:" + colorCode + "\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</span>";
            }
        };
        addTagToMap(backgroundColorTag);

        BBCodeTag fontTag = new BBCodeTag("font") {
            @Override
            public String getOpenTag(String param, String content) {
                if (param.isEmpty()) {
                    param = "'Open Sans','Noto Sans CJK TC','Noto Sans CJK SC','Noto Sans CJK JP','Noto Sans CJK KR','Lucida Grande',Tahoma,arial,sans-serif;";
                }
                return "<span style=\"font-family:" + param + "\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</span>";
            }
        };
        addTagToMap(fontTag);

        BBCodeTag sizeTag = new BBCodeTag("size") {
            @Override
            public String getOpenTag(String param, String content) {
                Pattern numberPattern = Pattern.compile("-?\\d+");
                Matcher matcher = numberPattern.matcher(param);
                if (matcher.find()) {
                    param = matcher.group();
                }
                if (param.isEmpty()) param = "14";
                return "<span style=\"font-size:" + param + "px;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</span>";
            }
        };
        addTagToMap(sizeTag);

        BBCodeTag centerTag = new BBCodeTag("center") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:center;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(centerTag);

        BBCodeTag leftTag = new BBCodeTag("left") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:left;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(leftTag);

        BBCodeTag rightTag = new BBCodeTag("right") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:right;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(rightTag);

        BBCodeTag justifyTag = new BBCodeTag("justify") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:justify;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(justifyTag);

        BBCodeTag startTag = new BBCodeTag("start") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:start;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(startTag);

        BBCodeTag endTag = new BBCodeTag("end") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<div style=\"text-align:end;\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</div>";
            }
        };
        addTagToMap(endTag);

        BBCodeTag imgTag = new BBCodeTag("img") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<img src=\"" + content + "\" \\/>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "";
            }
        };
        imgTag.setDisplayContent(false);
        addTagToMap(imgTag);

        BBCodeTag urlTag = new BBCodeTag("url") {
            @Override
            public String getOpenTag(String param, String content) {
                if (!param.toLowerCase().startsWith("javascript") && !param.startsWith("http://") && !param.startsWith("https://")) {
                    param = "https://" + param;
                }
                if (!urlPattern.matcher(param).find()) return "<a>";
                return "<a href=\"" + param + "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\" onmousedown=\"event.preventDefault();event.stopPropagation();\">";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</a>";
            }
        };
        addTagToMap(urlTag);

        BBCodeTag emailTag = new BBCodeTag("email") {
            @Override
            public String getOpenTag(String param, String content) {
                String email = param.isEmpty() ? content : param;
                if (!emailPattern.matcher(email).find()) return "<a>";

                return "<a href='mailto:" + email + "' onmousedown='event.preventDefault();event.stopPropagation();'>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</a>";
            }
        };
        addTagToMap(emailTag);

        BBCodeTag quoteTag = new BBCodeTag("quote") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<blockquote>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</blockquote>";
            }
        };
        addTagToMap(quoteTag);

        BBCodeTag brTag = new BBCodeTag("br") {
            @Override
            public String getOpenTag(String param, String content) {
                return "";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "<br>";
            }
        };
        addTagToMap(brTag);

        /*
         *  The [*] tag is special since the user does not define a closing [/*] tag when writing their bbcode.
         *  Instead this module parses the code and adds the closing [/*] tag in for them. None of the tags you
         *  add will act like this and this tag is an exception to the others.
         */
        BBCodeTag starTag = new BBCodeTag("*") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<li>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</li>";
            }
        };
        addTagToMap(starTag);
    }

    private String getCorrectColorCode(String colorCode) {
        if (!colorNamePattern.matcher(colorCode).find() && !colorRGBPattern.matcher(colorCode).find()) {
            if (!colorCodePattern.matcher(colorCode).find()) {
                colorCode = "black";
            } else {
                if (colorCode.charAt(0) != '#') {
                    colorCode = "#" + colorCode;
                }
            }
        }
        return colorCode;
    }

    public String parseToHTML(String text) {
        text = text.replace("$", "&#36;").replace("\\", "&#92;");

        StringBuilder source = new StringBuilder();
        source.append(text);

        try {
            int maxCount = 5000, count = 0;
            while (!text.equals((text = processEmptyTag(text))) && count++ < maxCount) { }  // process tags that don't have their content parsed
            while (!text.equals((text = fixStarTag(text))) && count++ < maxCount) { }       // add in closing tags for the [*] tag
            while (!text.equals((text = addBbcodeLevels(text))) && count++ < maxCount) { }  // add in level metadata
            while (!text.equals((text = processContent(text))) && count++ < maxCount) { }

            if (count > maxCount) {
                // return original content if there are some tag missed.
                return source.toString();
            }
        } catch (Exception ignored) {
            return source.toString();
        }

        return text.replace("&#36;", "$").replace("&#92;", "\\");
    }

    public String removeBBCodeTag(String text) {
        text = text.replace("$", "&#36;").replace("\\", "&#92;");

        StringBuilder source = new StringBuilder();
        source.append(text);

        try {
            int maxCount = 5000, count = 0;
            String prevText;
            do {
                prevText = text;
                Matcher matcher = parsedTagPattern.matcher(text);
                StringBuffer tempFiller = new StringBuffer();
                while (matcher.find()) {
                    if ("br".equalsIgnoreCase(matcher.group(1))) {
                        matcher.appendReplacement(tempFiller, "\r\n");
                    } else {
                        String content = matcher.group(3).replaceAll("\\[", "&#91;").replaceAll("\\]", "&#93;");
                        matcher.appendReplacement(tempFiller, content);
                    }
                }
                matcher.appendTail(tempFiller);
                text = tempFiller.toString();
            } while (!prevText.equals(text) && count++ < maxCount);

            if (count > maxCount) {
                // return original content if there are some tag missed.
                return source.toString();
            }
        } catch (Exception ignored) {
            return source.toString();
        }

        return text.replace("&#36;", "$").replace("&#92;", "\\");
    }

    private String processEmptyTag(String text) {
        Matcher matcher = noParsedTagPattern.matcher(text);
        StringBuffer tempFiller = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(3).replaceAll("\\[", "&#91;").replaceAll("\\]", "&#93;");
            String param = matcher.group(2) == null ? "" : matcher.group(2);
            matcher.appendReplacement(tempFiller,
                    "[" + matcher.group(1) + param + "]" + content + "[/" + matcher.group(1) + "]");
        }
        matcher.appendTail(tempFiller);
        return tempFiller.toString();
    }

    /*
     * The star tag [*] is special in that it does not use a closing tag. Since this parser requires that tags to have a closing
     * tag, we must pre-process the input and add in closing tags [/*] for the star tag.
     * We have a little levaridge in that we know the text we're processing wont contain the <> characters (they have been
     * changed into their HTML entity form to prevent XSS and code injection), so we can use those characters as markers to
     * help us define boundaries and figure out where to place the [/*] tags.
     */
    private String fixStarTag(String text) {
        StringBuffer tempFiller = new StringBuffer();
        Matcher matcher = Pattern.compile("\\[list([ =][^\\]]*)?\\]([^>]*?)(\\[\\/list])").matcher(text);
        while (matcher.find()) {
            String innerListTxt = matcher.group();
            while (!innerListTxt.equals((innerListTxt = fixInnerStarTag(innerListTxt)))) {
            }
            matcher.appendReplacement(tempFiller, innerListTxt);
        }
        matcher.appendTail(tempFiller);

        return tempFiller.toString();
    }

    private String fixInnerStarTag(String innerListTxt) {
        Matcher innerMatcher = Pattern.compile("\\[\\*\\]([^\\[]*?)(\\[\\*\\]|\\[\\/list])").matcher(innerListTxt);
        StringBuffer innerTempFiller = new StringBuffer();
        while (innerMatcher.find()) {
            String endTag = innerMatcher.group(2);
            if (endTag.equalsIgnoreCase("[/list]")) {
                endTag = "[/*][/list]";
            } else {
                endTag = "[/*][*]";
            }
            innerMatcher.appendReplacement(innerTempFiller, "[*]" + innerMatcher.group(1) + endTag);
        }
        innerMatcher.appendTail(innerTempFiller);

        return innerTempFiller.toString();
    }

    private String addBbcodeLevels(String text) {
        Matcher matcher = parsedTagPattern.matcher(text);
        StringBuffer tempFiller = new StringBuffer();
        while (matcher.find()) {
            String matchStr = matcher.group().replaceAll("\\[", "<").replaceAll("\\]", ">");
            matcher.appendReplacement(tempFiller, updateTagDepths(matchStr));
        }
        matcher.appendTail(tempFiller);

        return tempFiller.toString();
    }

    /*
     *  This function updates or adds a piece of metadata to each tag called "bbcl" which
     *  indicates how deeply nested a particular tag was in the bbcode. This property is removed
     *  from the HTML code tags at the end of the processing.
     */
    private String updateTagDepths(String tagContents) {
        Matcher matcher = checkDepthsPattern.matcher(tagContents);
        Matcher subMatcher;
        StringBuffer tempFiller = new StringBuffer();
        while (matcher.find()) {
            String subMatchStr = matcher.group(1);
            subMatcher = subDepthsComparePattern.matcher(subMatchStr);

            if (!subMatcher.find()) {
                matcher.appendReplacement(tempFiller, "<bbcl=0 " + subMatchStr + ">");
            } else {
                StringBuffer subTempFiller = new StringBuffer();
                subMatcher = subDepthsPattern.matcher(subMatchStr);
                while (subMatcher.find()) {
                    subMatcher.appendReplacement(subTempFiller, subMatcher.group(1) + (Integer.parseInt(subMatcher.group(2)) + 1));
                }
                subMatcher.appendTail(subTempFiller);

                matcher.appendReplacement(tempFiller, "<" + subTempFiller.toString() + ">");
            }
        }
        matcher.appendTail(tempFiller);

        return tempFiller.toString();
    }

    private String processContent(String text) {
        Matcher bbMatcher = bbclPattern.matcher(text);
        StringBuffer tempFiller = new StringBuffer();
        try {
            while (bbMatcher.find()) {
                String tagName = bbMatcher.group(2).toLowerCase(), tagParam = bbMatcher.group(3), tagContents = bbMatcher.group(4);
                if (tagParam == null) tagParam = "";
                else {
                    tagParam = tagParam.trim().substring(1);
                } // remove "="

                BBCodeTag tag = tagMap.get(tagName);

                String processedContent = tag.isNoParse() ? unprocess(tagContents) : processContent(tagContents),
                        openTag = tag.getOpenTag(tagParam, processedContent),
                        closeTag = tag.getEndTag(tagParam, processedContent);

                if (!tag.isDisplayContent()) {
                    processedContent = "";
                }

                bbMatcher.appendReplacement(tempFiller, openTag + processedContent + closeTag);
            }
        } catch (StackOverflowError e) { // avoid exception generated from regex pattern
            System.out.println("StackOverflowError");
        }
        bbMatcher.appendTail(tempFiller);

        return tempFiller.toString();
    }

    /*
     *  This function removes the metadata added by the updateTagDepths function
     */
    private String unprocess(String tagContent) {
        return tagContent.replaceAll("<bbcl=[0-9]+ \\/\\*>", "").replaceAll("<bbcl=[0-9]+ ", "&#91;").replace(">", "&#93;");
    }

    private void addTagToMap(String tagName) {
        if (!tagName.isEmpty()) {
            addTagToMap(new BBCodeTag(tagName));
        }
    }

    private void addTagToMap(BBCodeTag tag) {
        tagMap.put(tag.getTagName(), tag);
    }
}

class BBCodeTag {
    private String tagName;
    private boolean noParse = false;
    private boolean displayContent = true;

    BBCodeTag(String tagName) {
        this.tagName = tagName;
    }

    public String getOpenTag(String param, String content) {
        return "<" + tagName + ">";
    }

    public String getEndTag(String param, String content) {
        return "</" + tagName + ">";
    }

    public String getTagName() {
        return tagName;
    }

    public boolean isNoParse() {
        return noParse;
    }

    public void setNoParse(boolean noParse) {
        this.noParse = noParse;
    }

    public boolean isDisplayContent() {
        return displayContent;
    }

    public void setDisplayContent(boolean displayContent) {
        this.displayContent = displayContent;
    }
}