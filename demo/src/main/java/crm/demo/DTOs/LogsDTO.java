package crm.demo.DTOs;

import crm.demo.Enteties.Logs;
import java.time.LocalDate;

public record LogsDTO(
        Long id,
        Long projectId,
        Long userId,
        String userNick,
        String logText,
        LocalDate logDateTime
) {
    public static LogsDTO from(Logs log) {
        return new LogsDTO(
                log.getId(),
                log.getProject().getId(),
                log.getUser().getId(),
                log.getUser().getNickName(),
                log.getLogText(),
                log.getLogDateTime()
        );
    }
}
