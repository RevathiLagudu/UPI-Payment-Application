package com.amzur.test.controller

import com.amzur.test.model.BankAccountModel
import com.amzur.test.service.BankAccountService
import com.amzur.test.service.KafkaProducerClient
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

import javassist.NotFoundException

import javax.inject.Inject

@Controller('/banks')
class BankAccountController {
    @Inject
    BankAccountService bankAccountService

    @Inject
    KafkaProducerClient kafkaProducerClient

    @Post('/link')
    def createBankAccount(@Body BankAccountModel bankAccountModel){
        return bankAccountService.createBankAccount(bankAccountModel)
    }

    @Delete('/{id}')
    def deleteBankAccount(@PathVariable Long id) {
        boolean success = bankAccountService.deleteBankAccount(id)
        if (success) {
            return [status: 'success', message: 'Bank account deleted successfully']
        } else {
            return [status: 'failure', message: 'Bank account not found']
        }
    }

    @Patch('/{id}/transactionLimit')
    def updateTransactionLimit(@PathVariable Long id, @Body Map<String, BigDecimal> requestBody) {
        BigDecimal newLimit = requestBody.get("transactionLimit")
        def updatedAccount = bankAccountService.updateTransactionLimit(id, newLimit)
        if (updatedAccount) {
            return [status: 'success', message: 'Transaction limit updated successfully', bankAccount: updatedAccount]
        } else {
            return [status: 'failure', message: 'Bank account not found']
        }
    }

    @Patch('/{id}/upiPin')
    def updateUpiPin(@PathVariable Long id, @Body Map<String, String> requestBody) {
        String newUpiPin = requestBody.get("upiPin")
        def updatedAccount = bankAccountService.updateUpiPin(id, newUpiPin)
        if (updatedAccount) {
            return [status: 'success', message: 'UPI PIN updated successfully', bankAccount: updatedAccount]
        } else {
            return [status: 'failure', message: 'Bank account not found']
        }
    }
    @Get('/accounts/{userId}')
    def allAccounts(@PathVariable Long userId){
        return bankAccountService.getAllAccounts(userId)
    }

    @Put("/setPrimary/{accountId}")
    HttpResponse<?> setPrimaryAccount(@PathVariable Long accountId) {
        try {
            def result = bankAccountService.setPrimaryAccount(accountId)
            return HttpResponse.ok(result)
        } catch (Exception e) {
            return HttpResponse.serverError([error: "Failed to set primary account", message: e.message])
        }
    }
    @Post("/sendMoney/{accountId}")
    def sendMoney(@PathVariable Long accountId, @Body Map<String, Object> request) {
        try {

            String upiPin = request.upiPin
            BigDecimal amount = new BigDecimal(request.amount as BigInteger)
            String receiverMobileNumber = request.receiverMobile
            def transactionResult = bankAccountService.sendMoney(accountId, upiPin, amount, receiverMobileNumber)

            String message = """{
            "transaction Id": "${transactionResult.id}",
            "recipient": "$receiverMobileNumber",
            "amount": "$amount",
             "date":"${transactionResult.date}"
        }"""
            kafkaProducerClient.sendMessage("payment-topic", message)
            return HttpResponse.ok(transactionResult)
        } catch (NotFoundException e) {
            return HttpResponse.notFound(e.message)
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(e.message)
        } catch (Exception e) {
            return HttpResponse.serverError("An error occurred: ${e.message}")
        }
    }

}
