package com.example.demo.service;

import com.example.demo.model.AppRole;
import com.example.demo.model.AppUser;
import com.example.demo.model.VerificationTokenUserAdmin;
import com.example.demo.repository.AppUserRepo;
import com.example.demo.repository.VerificationTokenUserAdminRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
public class UserServiceImpl {

    @Value("${firstadmin}")
    private String FIRSTADMIN;

    private AppUserRepo appUserRepo;
    private PasswordEncoder passwordEncoder;
    private VerificationTokenUserAdminRepo verificationTokenUserAdminRepo;
    private MailSenderService mailSenderService;


    public UserServiceImpl(AppUserRepo appUserRepo, PasswordEncoder passwordEncoder, VerificationTokenUserAdminRepo verificationTokenUserAdminRepo, MailSenderService mailSenderService) {
        this.appUserRepo = appUserRepo;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenUserAdminRepo = verificationTokenUserAdminRepo;
        this.mailSenderService = mailSenderService;
    }

    public boolean addUser(AppUser appUser, HttpServletRequest request) {
        if (appUserRepo.findAllByUsername(appUser.getUsername()) == null) {
            boolean admin = false;
            VerificationTokenUserAdmin verificationTokenUser = new VerificationTokenUserAdmin();
            String valueUser = UUID.randomUUID().toString();
            verificationTokenUser.setValueUser(valueUser);

            String valueAdmin = UUID.randomUUID().toString();
            if (appUser.getRole() == AppRole.ADMIN) {
                admin = true;
                verificationTokenUser.setValueAdmin(valueAdmin);
            }
            appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
            appUser.setRole(AppRole.USER);
            appUser.setEnabled(false);
            appUserRepo.save(appUser);

            verificationTokenUser.setAppUser(appUser);
            verificationTokenUserAdminRepo.save(verificationTokenUser);

            String url = "http://" + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath()
                    + "/verifytoken?token=" + valueUser;
            try {
                mailSenderService.sendMail(appUser.getUsername(), "Verification Token", url, false);
            } catch (MessagingException e) {
                e.printStackTrace();
                return false;
            }

            if (admin) {
                String urlAdmin = "http://" + request.getServerName() + ":" + request.getServerPort()
                        + request.getContextPath()
                        + "/verifyadmin?token=" + valueAdmin;
                try {
                    mailSenderService.sendMail(FIRSTADMIN, "Verification Admin", urlAdmin, false);
                } catch (MessagingException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean verifytoken(String token) {

        VerificationTokenUserAdmin verification = new VerificationTokenUserAdmin();
        verification = verificationTokenUserAdminRepo.findByValueUser(token);
        if (verification != null) {
            AppUser user = verification.getAppUser();
            user.setEnabled(true);
            appUserRepo.save(user);
            String admin = verification.getValueAdmin();
            if (admin == null) {
                verificationTokenUserAdminRepo.deleteById(verification.getId());
            }
            return true;
        }
        return false;
    }

    public boolean verifytokenAdmin(String token) {
        VerificationTokenUserAdmin verification = new VerificationTokenUserAdmin();
        verification = verificationTokenUserAdminRepo.findByValueAdmin(token);
        if (verification != null) {
            AppUser user = verification.getAppUser();
            user.setRole(AppRole.ADMIN);
            user.setEnabled(true);
            appUserRepo.save(user);
            verificationTokenUserAdminRepo.deleteById(verification.getId());
            return true;
        }
        return false;
    }
}
