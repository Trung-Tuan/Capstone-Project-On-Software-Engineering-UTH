package com.uth.capstone.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity ánh xạ bảng Location (Địa điểm trong trường) trong cơ sở dữ liệu MySQL (uth_find_db).
 */
@Entity
@Table(name = "Location")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LocationID", nullable = false)
    private Integer locationId;

    @Column(name = "BuildingName", nullable = false, length = 100)
    private String buildingName;

    @Column(name = "Floor", length = 50)
    private String floor;

    @Column(name = "RoomZone", nullable = false, length = 100)
    private String roomZone;
}

