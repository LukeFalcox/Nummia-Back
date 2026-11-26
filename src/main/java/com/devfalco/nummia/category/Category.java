package com.devfalco.nummia.category;

import com.devfalco.nummia.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    /** Cor em hexadecimal (#RRGGBB) usada nos gráficos. */
    @Column(length = 7)
    private String color;

    /** Nome do ícone (Lucide Icons) usado no Front-End. */
    @Column(length = 40)
    private String icon;

    @Column(nullable = false)
    private boolean archived;
}
