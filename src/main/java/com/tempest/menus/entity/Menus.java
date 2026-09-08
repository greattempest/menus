package com.tempest.menus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_menus")
public class Menus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = 20)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 2)
  private String status;

  @Column(length = 500)
  private String image;

  @Column(length = 500)
  private String video;

  @Column(length = 500)
  private String material;

  @Column(columnDefinition = "TEXT")
  private String step;

  @Column(length = 500)
  private String energy;

  @Column(length = 300)
  private String tag;

  @Column(name = "delete_flag")
  private Integer deleteFlag = 0;

  public Menus() {
  }

  public Menus(Integer id, String code, String name, String status, String image, String video,
      String material, String step, String energy, String tag) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.status = status;
    this.image = image;
    this.video = video;
    this.material = material;
    this.step = step;
    this.energy = energy;
    this.tag = tag;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getVideo() {
    return video;
  }

  public void setVideo(String video) {
    this.video = video;
  }

  public String getMaterial() {
    return material;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public String getStep() {
    return step;
  }

  public void setStep(String step) {
    this.step = step;
  }

  public String getEnergy() {
    return energy;
  }

  public void setEnergy(String energy) {
    this.energy = energy;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public Integer getDeleteFlag() {
    return deleteFlag;
  }

  public void setDeleteFlag(Integer deleteFlag) {
    this.deleteFlag = deleteFlag;
  }
}
