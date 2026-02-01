package crm.demo.Enteties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Data
@Setter
@Entity
@Table(name = "project_messages")
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMessages {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customersMessages;

    @Column(name = "message_content")
    String messageContent;

    @Column(name = "message_author")
    String author;

    @Column (name = "message_date")
    LocalDate messageDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("messages")
    @JoinColumn(name = "project_id", nullable = false)
    Project project;
}
