package com.unisal.selectaapi.notificacoes.listener;

import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
public class NotificacaoListener {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public NotificacaoListener(SimpMessagingTemplate simpMessagingTemplate, UserRepository userRepository, CompanyMemberRepository companyMemberRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.userRepository = userRepository;
        this.companyMemberRepository = companyMemberRepository;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(CriarNotificacaoEvent e){
        var userRecebendo = userRepository.findById(e.userId());
        if(UserRole.CANDIDATE.name().equals(userRecebendo.get().getRole())){
                        simpMessagingTemplate.convertAndSendToUser(e.userId().toString(),
                                "/queue/notificacoes", e.response());

        }else{
            var membrosExtrasParaAvisar = companyMemberRepository.findAllByCompany_User_Id(userRecebendo.get().getId());
            var usersParaAvisar = membrosExtrasParaAvisar.stream().map(membro ->membro.getUser().getId()).toList();
            usersParaAvisar.add(e.userId());
            usersParaAvisar.forEach(userId ->simpMessagingTemplate.convertAndSendToUser(userId.toString(),
                            "/queue/notificacoes", e.response()
                            )
            );


        }
    }
}
