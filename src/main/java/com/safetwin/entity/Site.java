package com.safetwin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Site extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(name = "biz_number", length = 20)
    private String bizNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    public enum Status {
        ACTIVE, INACTIVE, COMPLETED
    }

    public void update(String name, String address, String bizNumber, Status status) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (bizNumber != null) this.bizNumber = bizNumber;
        if (status != null) this.status = status;
    }
}
