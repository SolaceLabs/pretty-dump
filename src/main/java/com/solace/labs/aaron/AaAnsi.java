/*
 * Copyright 2023 Solace Corporation. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.solace.labs.aaron;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.AnsiConsole;

/**
 * Kind of a wrapper around the JAnsi library.  Modified for my own uses.
 * @author Aaron Lee
 */
public class AaAnsi {

	enum ColorMode {
		OFF,
		MINIMAL,
		STANDARD,
		VIVID,
		LIGHT,
		;
	}

	private static ColorMode MODE = ColorMode.STANDARD;
	static {
		if (System.getenv("PRETTY_COLORS") != null) {
			try {
				MODE = ColorMode.valueOf(System.getenv("PRETTY_COLORS").toUpperCase());
				Elem.updateColors(MODE);
			} catch (IllegalArgumentException e) { }
		}
	}
	
	static ColorMode getColorMode() {
		return MODE;
	}
	
	private Ansi ansi = new Ansi();
	private Elem curElem = null;  // not yet inside anything
	
	private boolean isOn() {
		return (MODE != ColorMode.OFF);
	}

	public AaAnsi() {
		reset();
	}
	
	public AaAnsi colorizeTopic(String topic) {
		if (MODE == ColorMode.VIVID) return colorizeTopicRainbow(topic);
		else return colorizeTopicPlain(topic);
	}
	
	private AaAnsi colorizeTopicPlain(String topic) {
		String[] levels = topic.split("/");
		for (int i=0; i<levels.length; i++) {
			fg(Elem.DESTINATION).a(levels[i]);
			if (i < levels.length-1) {
				fg(Elem.TOPIC_SEPARATOR).a('/');//.reset();
			}
		}
		return this;
	}
	
//	private static int maxLevels = 1;
	static LinkedListOfIntegers maxLengthTopicLevels = new LinkedListOfIntegers();
	private AaAnsi colorizeTopicRainbow(String topic) {
		String[] levels = topic.split("/");
//		maxLevels = Math.max(maxLevels, levels.length);
		maxLengthTopicLevels.insert(levels.length);
		// https://github.com/topics/256-colors
		int[] colorTable = new int[] { 82, 83, 84, 85, 86, 87,
				                       81, 75, 69, 63, /*57,*/
				                       93, 129, 165, 201,
				                       200, 199, 198, 197,
				                       203, 209, 215, 221, 227,
				                       191, 155, 119
		};
		int startingColIndex = 5;  // cyan 86
//		int startingColIndex = 7;  // temp 75
		double step = Math.max(1, Math.min(3, colorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.getMax())));
		for (int i=0; i<levels.length; i++) {
			ansi.fg(colorTable[(startingColIndex + (int)(step * i)) % colorTable.length]).a(levels[i]);
			if (i < levels.length-1) {
				fg(Elem.TOPIC_SEPARATOR).a('/').reset();
			}
		}
		return this;
	}
	
	public AaAnsi invalid(String s) {
		if (isOn()) {
			fg(Elem.ERROR).a(s).reset();
		} else {
			ansi.a(s);
		}
		return this;
	}
	
	public AaAnsi ex(Exception e) {
		String exception = e.getClass().getSimpleName() + " - " + e.getMessage();
		return invalid(exception);
	}
	
	public AaAnsi fg(Elem elem) {
		curElem = elem;
		if (isOn()) {
			Col c = elem.getCurrentColor();
			if (c.faint) {
				makeFaint().fg(c.value);
			} else if (c.italics) {
				makeItalics().fg(c.value);
			} else {
				fg(c.value);
			}
		}
		return this;
	}
	
	public AaAnsi fg(int colorIndex) {
		if (isOn()) {
			if (colorIndex == -1) ansi.fgDefault();
			else ansi.fg(colorIndex);
		}
		return this;
	}
	
	AaAnsi makeFaint() {
		if (isOn()) {
			/* if (faint)*/ ansi.a(Attribute.INTENSITY_FAINT);
//			else ansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
		}
		return this;
	}

	public AaAnsi makeItalics() {
		ansi.a(Attribute.ITALIC);
//		if (isOn()) {
//			/* if (faint)*/ ansi.a(Attribute.INTENSITY_FAINT);
////			else ansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
//		}
		return this;
	}

	@Override
	public String toString() {
		return UsefulUtils.chop(ansi.toString());// + (isOn() ? new Ansi().reset().toString() : "");
//		return UsefulUtils.chop(ansi.toString()) + new Ansi().reset().toString();
	}

	/** Just jam is straight in, don't parse at all! */
	public AaAnsi aRaw(String s) {
		ansi.a(s);
		return this;
	}

	/** Just copy the whole AaAnsi into this one. */
	public AaAnsi a(AaAnsi ansi) {
		aRaw(ansi.toString());
		return this;
	}
	
	public AaAnsi a(char c) {
		ansi.a(c);
		return this;
	}

	public AaAnsi a(boolean b) {
		ansi.a(Boolean.toString(b));
		return this;
	}

	public AaAnsi a(String s) {
		return a(s, false, false);
	}

	public AaAnsi a(String s, boolean compact) {
		return a(s, compact, false);
	}

	/** Consider each char individually, and if replacement \ufffd char then add some red colour.
	 *  Also, compact means replace all the invisible whitespace (CR, LF, TAB) with dots
	 */
	public AaAnsi a(String s, boolean compact, boolean styled) {
		StringBuilder sb = new StringBuilder();
		String replacement = null;  // needed if we have to substitute any invalid chars, init later if required
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 || c == 0x7f) {  // special handling of control characters, make them visible
//				if (compact || (!compact && (c < 0x09 || c == 0x0B || c == 0x0C || c > 0x0D))) {
				if (compact || c < 0x09 || c > 0x0A) {
//				if (compact || c != 0x0A) {
					if (c == 0) {
						sb.append('∅');  // make NULL more visible
//						sb.append('Ø');  // make NULL more visible
//						sb.append("∅ Ø Ø");
//						if (isOn()) {
//							Ansi a = new Ansi().fg(Elem.NULL.getCurrentColor().value).a('∅').fg(curElem.getCurrentColor().value);
//							sb.append(a.toString());
//						} else {
//							sb.append('∅');
//						}
					} else if (c == 2899) {  // don't do this... was test code for making tabs visible
							if (isOn()) {
							Ansi a = new Ansi().a(Attribute.INTENSITY_FAINT).a("────────").reset().fg(curElem.getCurrentColor().value);
							sb.append(a.toString());
						} else {
							sb.append("────────");
						}
					} else {
						sb.append("·");  // control chars
					}
				} else {  // 0x9 is tab, 0xa is line feed
					sb.append(c);
				}
			} else if (c == '\ufffd') {  // the replacement char introduced when trying to parse the bytes with the specified charset
				if (replacement == null) {  // lazy initialization
					if (isOn()) {  // bright red background, upsidedown ?
						Ansi a = new Ansi().reset().bg(Elem.ERROR.getCurrentColor().value).fg(231).a('¿').bgDefault().fg(curElem.getCurrentColor().value);
						replacement = a.toString();
					} else {
						replacement = "¿";
					}
				}
				sb.append(replacement);
			} else {  // all good, normal char
				if (styled && isOn()) {
					if (Character.isMirrored(c)) {
						AaAnsi a = new AaAnsi().fg(Elem.BRACE).a(c).fg(curElem);
						sb.append(a.toString());
					} else if ((c == ',' || c == ';' || c == '.' || c == ':') && i < s.length()-1 && Character.isWhitespace(s.charAt(i+1))) {
						AaAnsi a = new AaAnsi().reset().a(c).fg(curElem);
//						a.ansi.fgDefault().a(c);
//						a.fg(curElem);
						sb.append(a.toString());
					} else {
						sb.append(c);
					}
				} else sb.append(c);
			}
		}
		ansi.a(sb.toString());
		return this;
	}
	
	/**
	 * I think this was supposed to be for quotes and stuff
	 */
//	public AaAnsi bookend(String s) {
//		if (s != null && s.length() > 2 && s.charAt(0) == s.charAt(s.length()-1)) {
//			makeFaintfaint(true);
//			ansi.a(s.charAt(0));
//			makeFaintfaint(false);
//			ansi.a(s.substring(1, s.length()-1));
//			makeFaintfaint(true);
//			ansi.a(s.charAt(s.length()-1));
//			makeFaintfaint(false);
//		} else {
//			ansi.a(s);
//		}
//		return this;
//	}

//	private void restore() {
//		if (isOn() && curElem != null) {
//			ansi.bgDefault();
//			fg(curElem);
//		}
//	}

	public AaAnsi reset() {
		if (isOn()) {
			ansi.reset();
			if (Elem.DEFAULT.getCurrentColor().value != -1) ansi.fg(Elem.DEFAULT.getCurrentColor().value);
		}
		return this;
	}
	
	public static void main(String... args) {
//		test();
	}
	
	static void test() {
		
		System.out.println("width: " + AnsiConsole.out().getTerminalWidth());

		System.out.println("colors: " + AnsiConsole.out().getColors());
		System.out.println("MODE: " + AnsiConsole.out().getMode());
		System.out.println("type: " + AnsiConsole.out().getType());

		Ansi ansi = new Ansi();
		for (int i=0;i<16;i++) ansi.fg(i).a("█████████");
		System.out.println(ansi.toString());
		
		ansi = new Ansi();
		for (int i=0;i<256;i++) ansi.fgRgb(i, 0, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(i, i, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, i, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, i, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, 0, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(i, 0, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fg(i).a("█");
		ansi.a('\n');
		ansi.fgRgb(0, 200, 149).a("solace███ int 43 closest 00d7af   ");
		ansi.fgRgb(0, 255, 190).a("bright solace███ int 49 00ffaf");
		ansi.a('\n');
		ansi.reset();
//		AnsiConsole.out().println(ansi);
		System.out.println(ansi.toString());
//		System.out.println(new AaAnsi().setSolaceGreen().a("AARONSOLACEMANUAL").reset());
		
	}

}
