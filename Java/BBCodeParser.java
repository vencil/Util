/*
This file is a java copy of Extendible-BBCode-Parser(https://github.com/patorjk/Extendible-BBCode-Parser)

Copyright (C) 2016 by Vencil(vencsvencil@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/


package demo.vencs;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBCodeParser {
    static BBCodeParser instance;
    static HashMap<String, BBCodeTag> tagMap;
    static Pattern urlPattern, colorNamePattern, colorCodePattern, emailPattern,
            bbclPattern, parsedTagPattern, noParsedTagPattern, openTags, closeTags,
            checkDepthsPattern, subDepthsComparePattern, subDepthsPattern;

    private BBCodeParser() {
        initTags();

        urlPattern = Pattern.compile("^(?:https?|file|c):(?:\\/{1,3}|\\\\{1})[-a-zA-Z0-9:;@#%&()~_?\\+=\\/\\\\\\.]*$");
        colorNamePattern = Pattern.compile("^(?:aliceblue|antiquewhite|aqua|aquamarine|azure|beige|bisque|black|blanchedalmond|blue|blueviolet|brown|burlywood|cadetblue|chartreuse|chocolate|coral|cornflowerblue|cornsilk|crimson|cyan|darkblue|darkcyan|darkgoldenrod|darkgray|darkgreen|darkkhaki|darkmagenta|darkolivegreen|darkorange|darkorchid|darkred|darksalmon|darkseagreen|darkslateblue|darkslategray|darkturquoise|darkviolet|deeppink|deepskyblue|dimgray|dodgerblue|firebrick|floralwhite|forestgreen|fuchsia|gainsboro|ghostwhite|gold|goldenrod|gray|green|greenyellow|honeydew|hotpink|indianred|indigo|ivory|khaki|lavender|lavenderblush|lawngreen|lemonchiffon|lightblue|lightcoral|lightcyan|lightgoldenrodyellow|lightgray|lightgreen|lightpink|lightsalmon|lightseagreen|lightskyblue|lightslategray|lightsteelblue|lightyellow|lime|limegreen|linen|magenta|maroon|mediumaquamarine|mediumblue|mediumorchid|mediumpurple|mediumseagreen|mediumslateblue|mediumspringgreen|mediumturquoise|mediumvioletred|midnightblue|mintcream|mistyrose|moccasin|navajowhite|navy|oldlace|olive|olivedrab|orange|orangered|orchid|palegoldenrod|palegreen|paleturquoise|palevioletred|papayawhip|peachpuff|peru|pink|plum|powderblue|purple|red|rosybrown|royalblue|saddlebrown|salmon|sandybrown|seagreen|seashell|sienna|silver|skyblue|slateblue|slategray|snow|springgreen|steelblue|tan|teal|thistle|tomato|turquoise|violet|wheat|white|whitesmoke|yellow|yellowgreen)$");
        colorCodePattern = Pattern.compile("^#?[a-fA-F0-9]{6}$");
        emailPattern = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

        StringBuilder parsedTagList = new StringBuilder();
        StringBuilder noParsedTagList = new StringBuilder();
        StringBuilder closeTagList = new StringBuilder();
        for (BBCodeTag bbCodeTag : tagMap.values()) {
            String tagName = bbCodeTag.getTagName();
            if (tagName.equals("*")) {
                tagName = "\\" + tagName;
            } else {
                closeTagList.append("/" + tagName + "|");
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
        if (closeTagList.length() > 0) {
            closeTagList.setLength(closeTagList.length() - 1);
        }

        bbclPattern = Pattern.compile("<bbcl=([0-9]+) (" + parsedTagList + ")([ =][^>]*?)?>((?:.|[\\r\\n])*?)<bbcl=\\1 \\/\\2>");
        parsedTagPattern = Pattern.compile("\\[(" + parsedTagList + ")([ =][^\\]]*?)?\\]([^\\[]*?)\\[\\/\\1\\]");
        noParsedTagPattern = Pattern.compile("\\[(" + noParsedTagList + ")([ =][^\\]]*?)?\\]([\\s\\S]*?)\\[\\/\\1\\]");
        openTags = Pattern.compile("(\\[)((?:" + parsedTagList + ")(?:[ =][^\\]]*?)?)(\\])");
        closeTags = Pattern.compile("(\\[)(" + closeTagList + ")(\\])");

        checkDepthsPattern = Pattern.compile("\\<([^\\>][^\\>]*?)\\>");
        subDepthsComparePattern = Pattern.compile("^bbcl=([0-9]+) ");
        subDepthsPattern = Pattern.compile("^(bbcl=)([0-9]+)");
    }

    public static BBCodeParser getInstance() {
        if (instance == null) {
            instance = new BBCodeParser();
        }

        return instance;
    }

    private void initTags() {
        tagMap = new HashMap<>();

        addTagToMap("b");
        addTagToMap("i");
        addTagToMap("u");
        addTagToMap("s");
        addTagToMap("sub");
        addTagToMap("sup");

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
        addTagToMap("td");
        addTagToMap("th");
        addTagToMap("tr");

        BBCodeTag olTag = new BBCodeTag("ol") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<ol style='list-style-type:" + param + "'>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</ol>";
            }
        };
        addTagToMap(olTag);

        BBCodeTag ulTag = new BBCodeTag("ul") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<ul style='list-style-type:" + param + "'>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</ul>";
            }
        };
        addTagToMap(ulTag);

        BBCodeTag listTag = new BBCodeTag("list") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<ul style='list-style-type:" + param + "'>";
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
                if (!colorNamePattern.matcher(colorCode).find()) {
                    if (!colorCodePattern.matcher(colorCode).find()) {
                        colorCode = "black";
                    } else {
                        if (colorCode.charAt(0) != '#') {
                            colorCode = "#" + colorCode;
                        }
                    }
                }
                return "<span style='color:" + colorCode + "'>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</span>";
            }
        };
        addTagToMap(colorTag);

        BBCodeTag imgTag = new BBCodeTag("img") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<img src='" + content + "' \\/>";
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
                if(!urlPattern.matcher(param).find()) return "<a>";
                return "<a onmousedown='window.open(\"" + param + "\");' href='" + param + "'>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</a>";
            }
        };
        addTagToMap(urlTag);

        BBCodeTag embedTag = new BBCodeTag("embed") {
            @Override
            public String getOpenTag(String param, String content) {
                return "<embed width=600 height=500 wmode=transparent src='" + content + "'\\/>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "";
            }
        };
        embedTag.setDisplayContent(false);
        addTagToMap(embedTag);

        BBCodeTag emailTag = new BBCodeTag("email") {
            @Override
            public String getOpenTag(String param, String content) {
                String email = param.isEmpty() ? content : param;
                if (!emailPattern.matcher(email).find()) return "<a>";

                return "<a href='mailto:" + email + "' onmousedown=\"window.open(\'mailto:" + email + "\');\">";
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
                return "<blockquote><p>";
            }

            @Override
            public String getEndTag(String param, String content) {
                return "</p></blockquote>";
            }
        };
        addTagToMap(quoteTag);

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

    public String parseToHTML(String text) {
        StringBuffer tempFiller = new StringBuffer();

        // escape HTML tag brackets
        text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        Matcher openMatcher, endMatcher;
        openMatcher = openTags.matcher(text);
        while (openMatcher.find()) {
            // group 1:'[' , group 2: tagName , group 3:']'
            openMatcher.appendReplacement(tempFiller, "<" + openMatcher.group(2) + ">");
        }
        openMatcher.appendTail(tempFiller);
        text = tempFiller.toString();
        tempFiller.setLength(0);

        endMatcher = closeTags.matcher(text);
        while (endMatcher.find()) {
            // group 1:'[' , group 2: end tagName , group 3:']'
            endMatcher.appendReplacement(tempFiller, "<" + endMatcher.group(2) + ">");
        }
        endMatcher.appendTail(tempFiller);
        text = tempFiller.toString();
        tempFiller.setLength(0);


        // escape ['s that aren't apart of tags
        text = text.replaceAll("\\[", "&#91;").replaceAll("\\]", "&#93;")
                .replaceAll("<", "[").replaceAll(">", "]");

        while (!text.equals((text = processEmptyTag(text)))) { }  // process tags that don't have their content parsed
        while (!text.equals((text = fixStarTag(text)))) { }       // add in closing tags for the [*] tag
        while (!text.equals((text = addBbcodeLevels(text)))) { }  // add in level metadata
        while (!text.equals((text = processContent(text)))) { }

        return text;
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
        text = text.replaceAll("\\[(?!\\*[ =\\]]|list([ =][^\\]]*)?\\]|\\/list[\\]])", "<")
                .replaceAll("\\[(?=list([ =][^\\]]*)?\\]|\\/list[\\]])", ">");

        StringBuffer tempFiller = new StringBuffer();
        Matcher matcher = Pattern.compile(">list([ =][^\\]]*)?\\]([^>]*?)(>\\/list])").matcher(text);
        while (matcher.find()) {
            String innerListTxt = matcher.group();
            while (!innerListTxt.equals((innerListTxt = fixinnerStarTag(innerListTxt)))) { }
            matcher.appendReplacement(tempFiller, innerListTxt.replaceAll(">", "<"));
        }
        matcher.appendTail(tempFiller);

        return tempFiller.toString().replaceAll("<", "[");
    }

    private String fixinnerStarTag(String innerListTxt) {
        Matcher innerMatcher = Pattern.compile("\\[\\*\\]([^\\[]*?)(\\[\\*\\]|>\\/list])").matcher(innerListTxt);
        StringBuffer innerTempFiller = new StringBuffer();
        while (innerMatcher.find()) {
            String endTag = innerMatcher.group(2);
            if (endTag.equalsIgnoreCase(">/list]")) {
                endTag = "</*]</list]";
            } else {
                endTag = "</*][*]";
            }
            innerMatcher.appendReplacement(innerTempFiller, "<*]" + innerMatcher.group(1) + endTag);
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
                    subMatcher.appendReplacement(subTempFiller, subMatcher.group(1) + (Encoder.parseInt(subMatcher.group(2)) + 1));
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
        while (bbMatcher.find()) {
            String tagName = bbMatcher.group(2).toLowerCase(), tagParam = bbMatcher.group(3), tagContents = bbMatcher.group(4);
            if (tagParam == null) tagParam = "";
            else { tagParam = tagParam.trim().substring(1); } // remove "="

            BBCodeTag tag = tagMap.get(tagName);
            String processedContent = tag.isNoParse() ? unprocess(tagContents) : processContent(tagContents),
                    openTag = tag.getOpenTag(tagParam, processedContent),
                    closeTag = tag.getEndTag(tagParam, processedContent);

            if (!tag.isDisplayContent()) {
                processedContent = "";
            }

            bbMatcher.appendReplacement(tempFiller, openTag + processedContent + closeTag);
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
    String tagName = "";
    boolean noParse = false;
    boolean displayContent = true;

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