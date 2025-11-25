package com.mp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institute_name", nullable = false)
    private String instituteName;

    @Column(name = "institute_location", nullable = false)
    private String instituteLocation;

    @Column(name = "institute_image")
    private String instituteImage;   // stored as URL or file path

    public Institution() {}

    public Institution(Long id, String instituteName, String instituteLocation, String instituteImage) {
        this.id = id;
        this.instituteName = instituteName;
        this.instituteLocation = instituteLocation;
        this.instituteImage = instituteImage;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstituteName() { return instituteName; }
    public void setInstituteName(String instituteName) { this.instituteName = instituteName; }

    public String getInstituteLocation() { return instituteLocation; }
    public void setInstituteLocation(String instituteLocation) { this.instituteLocation = instituteLocation; }

    public String getInstituteImage() { return instituteImage; }
    public void setInstituteImage(String instituteImage) { this.instituteImage = instituteImage; }
}
