package com.unisal.selectaapi.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.account.entity.UserSettings;
import com.unisal.selectaapi.account.repository.UserSettingsRepository;
import com.unisal.selectaapi.user.entity.User;

@Service
public class UserSettingsService {

    private final UserSettingsRepository repository;

    public UserSettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    /** Contas antigas não têm linha de configurações: ela nasce com os padrões na primeira leitura. */
    @Transactional
    public UserSettings getOrCreate(User user) {
        return repository.findByUserId(user.getId())
                .orElseGet(() -> repository.save(new UserSettings(user)));
    }

    @Transactional(readOnly = true)
    public boolean isTwoFactorEnabled(User user) {
        return repository.findByUserId(user.getId()).map(UserSettings::isTwoFactorEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean wantsApplicationUpdateEmails(String email) {
        return repository.findByUserEmail(email).map(UserSettings::isApplicationUpdatesEmail).orElse(true);
    }
}
