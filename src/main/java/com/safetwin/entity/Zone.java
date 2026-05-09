package com.safetwin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Zone extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "floor_number")
    private Integer floorNumber;

    // 탑뷰 좌표 (이미지 대비 % 단위)
    private Double x;
    private Double y;
    private Double w;
    private Double h;

    @Column(length = 50)
    private String area;

    public void update(String name, String description, Integer floorNumber,
                       Double x, Double y, Double w, Double h, String area) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (floorNumber != null) this.floorNumber = floorNumber;
        if (x != null) this.x = x;
        if (y != null) this.y = y;
        if (w != null) this.w = w;
        if (h != null) this.h = h;
        if (area != null) this.area = area;
    }
}
