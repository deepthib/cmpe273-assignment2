package edu.sjsu.cmpe.procurement.domain;

import java.net.URL;


public class Book {
    private long isbn;
    private String title;
    private String Category;
    private URL coverimage;
    private String status;

    public String getCategory() {
		return Category;
	}

	public void setCategory(String category) {
		Category = category;
	}

	public URL getCoverimage() {
		return coverimage;
	}

	public void setCoverimage(URL coverimage) {
		this.coverimage = coverimage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
     * @return the isbn
     */
    public long getIsbn() {
	return isbn;
    }

    /**
     * @param isbn
     *            the isbn to set
     */
    public void setIsbn(long isbn) {
	this.isbn = isbn;
    }

    /**
     * @return the title
     */
    public String getTitle() {
	return title;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
	this.title = title;
    }
}
