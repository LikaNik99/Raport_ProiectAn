package crm.demo.Enteties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Entity
@Table(name = "Cities")
@AllArgsConstructor
@NoArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column
    String city;

    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    @JsonIgnore
    List<Project> project;
}
