package com.amzur.test.service

import com.amzur.test.constants.ApplicationConstants
import com.amzur.test.domain.UserDomain
import com.amzur.test.handler.UserNotFound
import com.amzur.test.model.UserModel
import grails.gorm.transactions.Transactional

import javax.inject.Singleton
import javax.persistence.EntityNotFoundException

@Singleton
class UserService {

    @Transactional
    def createUser(UserModel userModel) {
        // Convert UserModel to UserDomain using the toDomain method
        UserDomain userDomain = UserModel.toDomain(userModel)

        // Save the userDomain instance, ensuring it is persisted in the database
        userDomain = userDomain.save(flush: true)

        // Return the saved UserDomain or convert it to UserModel if needed
        return UserModel.toModel(userDomain)
    }


    @Transactional
    def findUser(String mobileNumber, String pin) {
        def userDomain = UserDomain.findByMobileNumberAndPin(mobileNumber, pin)

        if (!userDomain) {
            // You can throw a custom exception if the user is not found
            throw new UserNotFound(ApplicationConstants.USER_NOT_FOUND)
        }

        // Return the UserModel (or specific fields like firstName)
        return UserModel.toModel(userDomain)
    }


    @Transactional
    def updateUser(UserModel userModel) {
        // Convert UserModel to UserDomain using the toDomain method
        UserDomain userDomain = UserModel.toDomain(userModel)

        // Retrieve the existing UserDomain instance from the database
        UserDomain existingUserDomain = UserDomain.findById(userDomain.id)

        if (!existingUserDomain) {
            throw new EntityNotFoundException("User not found with id: ${userDomain.id}")
        }

        // Update the existingUserDomain instance with the new values from userDomain
        existingUserDomain.properties = userDomain.properties

        // Save the updated userDomain instance, ensuring changes are persisted in the database
        existingUserDomain = existingUserDomain.save(flush: true)

        // Return the updated UserDomain or convert it to UserModel if needed
        return UserModel.toModel(existingUserDomain)
    }
}
