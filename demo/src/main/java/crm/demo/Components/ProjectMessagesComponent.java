package crm.demo.Components;

import crm.demo.DTOs.CreateMessageRequest;
import crm.demo.DTOs.MessageDTO;
import crm.demo.DTOs.ProjectDTO;
import crm.demo.DTOs.ProjectMessageDTO;
import crm.demo.Enteties.*;
import crm.demo.Repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class ProjectMessagesComponent {
    private final ProjectMessagesRepository messagesRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMessagesRepository messageRepository;
    private final LogsRepository logRepository;
    private final CrmUserRepository crmUserRepository;

    @GetMapping("project-messages")
    public List<ProjectMessageDTO> getAllMessages(){
        return messagesRepository.findAll().stream()
                .map(ProjectMessageDTO::from)
                .toList();
    }
    @GetMapping("project-messages/project/{projectId}/get-messages")
    public List<ProjectMessageDTO> getMessagesByProjectID(@PathVariable("projectId") Long projectId){
        return messagesRepository.findAllByProjectId(projectId)
                .stream()
                .map(m -> new ProjectMessageDTO(
                        m.getId(),
                        m.getMessageContent(),
                        m.getAuthor(),
                        m.getMessageDate()
                ))
                .toList();
    }

    @PostMapping("project-messages/project/{projectId}/post-messages")
    public MessageDTO submitAMessage(@RequestBody CreateMessageRequest request, @PathVariable Long projectId, Principal principal){
        String username = principal.getName();
        CrmUser user = crmUserRepository.findByLogin(username).orElseThrow(() -> new RuntimeException("Customer not found"));
        // 2. Получаем проект
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        // 3. Создаём сообщение
        ProjectMessages message = new ProjectMessages();
        message.setMessageContent(request.getMessageContent());
        message.setAuthor(principal.getName());
        message.setMessageDate(LocalDate.now());
        message.setCustomersMessages(user.getCustomer());
        message.setProject(project);
        // 4. Сохраняем
        ProjectMessages saved = messagesRepository.save(message);
        MessageDTO mdto = mapToDto(saved);
        // 5. DTO
        return mdto;
    }

    private MessageDTO mapToDto(ProjectMessages message) {
        MessageDTO dto = new MessageDTO();
//        dto.setId(message.getId());
        dto.setMessageContent(message.getMessageContent());
        dto.setAuthor(message.getAuthor());
        dto.setMessageDate(message.getMessageDate());
        return dto;
    }
}
