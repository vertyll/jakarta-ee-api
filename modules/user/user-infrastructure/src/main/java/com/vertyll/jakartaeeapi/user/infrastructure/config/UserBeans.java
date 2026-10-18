package com.vertyll.jakartaeeapi.user.infrastructure.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.vertyll.jakartaeeapi.user.application.service.UserAccountService;
import com.vertyll.jakartaeeapi.user.domain.UserRepository;

/** Wires the user module's application to its adapters. */
@Singleton
public class UserBeans {

    @Produces
    @Singleton
    public UserAccountService userAccountService(UserRepository repository) {
        return new UserAccountService(repository);
    }
}
