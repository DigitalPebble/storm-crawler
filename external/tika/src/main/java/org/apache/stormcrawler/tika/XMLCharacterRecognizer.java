/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * XXX NUTCH COMMENT
 * XXX ab@apache.org: This class is copied verbatim from Xalan-J 2.6.0
 * XXX distribution, org.apache.xml.utils.DOMBuilder, in order to
 * avoid dependency on Xalan.
 */

package org.apache.stormcrawler.tika;

/**
 * Class used to verify whether the specified <var>ch</var> conforms to the XML 1.0 definition of
 * whitespace.
 */
class XMLCharacterRecognizer {

    private XMLCharacterRecognizer() {}

    /**
     * Returns whether the specified <var>ch</var> conforms to the XML 1.0 definition of whitespace.
     * Refer to <A href="http://www.w3.org/TR/1998/REC-xml-19980210#NT-S">the definition of <CODE>S
     * </CODE></A> for details.
     *
     * @param ch Character to check as XML whitespace.
     * @return =true if <var>ch</var> is XML whitespace; otherwise =false.
     */
    static boolean isWhiteSpace(char ch) {
        return (ch == 0x20) || (ch == 0x09) || (ch == 0xD) || (ch == 0xA);
    }

    /**
     * Tell if the string is whitespace.
     *
     * @param ch Character array to check as XML whitespace.
     * @param start Start index of characters in the array
     * @param length Number of characters in the array
     * @return True if the characters in the array are XML whitespace; otherwise, false.
     */
    static boolean isWhiteSpace(char ch[], int start, int length) {

        int end = start + length;

        for (int s = start; s < end; s++) {
            if (!isWhiteSpace(ch[s])) return false;
        }

        return true;
    }

    /**
     * Tell if the string is whitespace.
     *
     * @param buf StringBuffer to check as XML whitespace.
     * @return True if characters in buffer are XML whitespace, false otherwise
     */
    static boolean isWhiteSpace(StringBuffer buf) {

        int n = buf.length();

        for (int i = 0; i < n; i++) {
            if (!isWhiteSpace(buf.charAt(i))) return false;
        }

        return true;
    }

    /**
     * Tell if the string is whitespace.
     *
     * @param s String to check as XML whitespace.
     * @return True if characters in buffer are XML whitespace, false otherwise
     */
    static boolean isWhiteSpace(String s) {

        if (null != s) {
            int n = s.length();

            for (int i = 0; i < n; i++) {
                if (!isWhiteSpace(s.charAt(i))) return false;
            }
        }

        return true;
    }
}
