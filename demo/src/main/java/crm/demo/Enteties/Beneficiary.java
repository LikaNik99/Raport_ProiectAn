package crm.demo.Enteties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Data
@Setter
@Entity
@Table(name = "Beneficiary")
@AllArgsConstructor
@NoArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column
    String benificiatyFirstName;
    @Column
    String companyName;
    @OneToMany(mappedBy = "beneficiary", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Project> projects;
}
