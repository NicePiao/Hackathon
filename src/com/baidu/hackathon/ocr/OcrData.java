package com.baidu.hackathon.ocr;

import java.util.List;

public class OcrData {
	private int errno;
	private String errmas;
	private String querysign;
	private List<OcrRet> ret;

	public int getErrno() {
		return errno;
	}

	public void setErrno(int errno) {
		this.errno = errno;
	}

	public String getErrmas() {
		return errmas;
	}

	public void setErrmas(String errmas) {
		this.errmas = errmas;
	}

	public String getQuerysign() {
		return querysign;
	}

	public void setQuerysign(String querysign) {
		this.querysign = querysign;
	}

	public List<OcrRet> getRet() {
		return ret;
	}

	public void setRet(List<OcrRet> ret) {
		this.ret = ret;
	}

	public static class OcrRet {
		private Rect rect;
		private String word;

		// prob

		public Rect getRect() {
			return rect;
		}

		public void setRect(Rect rect) {
			this.rect = rect;
		}

		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		public static class Rect {
			private int left;
			private int top;
			private int width;
			private int height;

			public int getLeft() {
				return left;
			}

			public void setLeft(int left) {
				this.left = left;
			}

			public int getTop() {
				return top;
			}

			public void setTop(int top) {
				this.top = top;
			}

			public int getWidth() {
				return width;
			}

			public void setWidth(int width) {
				this.width = width;
			}

			public int getHeight() {
				return height;
			}

			public void setHeight(int height) {
				this.height = height;
			}
		}
	}

}
