package crm.demo.Enteties;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Data
@Entity
@Table(name = "Project")
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column
    String projectName;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate deadLine;

    @Column
    String projectDescription;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    Customer owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private Beneficiary beneficiary;

    @Column
    Long budget;

    @Column
    String creatorName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<ProjectMessages> messagesList;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Logs> logs;
}
