package example.model;

import java.util.UUID;

public class Artist {
	private String name;
	private String plays;
	private Float price;
	private static String staticInfo;
	private int id;

	public String getName() {
		return name;
	}

	public void setName(String data) {
		name = data;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float data) {
		price = data;
	}

	public String getPlays() {
		return plays;
	}

	public void setPlays(String data) {
		plays = data;
	}

	public int getId() {
		return id;
	}

	public void setId(int data) {
		id = data;
	}

	public Artist() {
		staticInfo = UUID.randomUUID().toString();
	}

	public /* static */ String getStaticInfo() {
		return Artist.staticInfo;
	}

	public Artist(int id, String name, String plays) {
		super();
		if (Artist.staticInfo == null) {
			Artist.staticInfo = UUID.randomUUID().toString();
		}
		this.name = name;
		this.id = id;
		this.plays = plays;
	}

}
