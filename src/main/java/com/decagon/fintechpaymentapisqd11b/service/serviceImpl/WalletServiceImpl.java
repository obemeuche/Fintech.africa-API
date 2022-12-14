package com.decagon.fintechpaymentapisqd11b.service.serviceImpl;

import com.decagon.fintechpaymentapisqd11b.dto.AccountFundDTO;
import com.decagon.fintechpaymentapisqd11b.dto.WalletDto;
import com.decagon.fintechpaymentapisqd11b.entities.Users;
import com.decagon.fintechpaymentapisqd11b.entities.Wallet;
import com.decagon.fintechpaymentapisqd11b.repository.UsersRepository;
import com.decagon.fintechpaymentapisqd11b.repository.WalletRepository;
import com.decagon.fintechpaymentapisqd11b.request.FlwWalletRequest;
import com.decagon.fintechpaymentapisqd11b.response.BaseResponse;
import com.decagon.fintechpaymentapisqd11b.response.FlwVirtualAccountResponse;
import com.decagon.fintechpaymentapisqd11b.security.filter.JwtUtils;
import com.decagon.fintechpaymentapisqd11b.service.LoginService;
import com.decagon.fintechpaymentapisqd11b.service.WalletService;
import com.decagon.fintechpaymentapisqd11b.util.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor @Slf4j

public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UsersRepository usersRepository;

   private final JwtUtils jwtUtils;
    private  String userToken = "";

    @Override
    public Wallet createWallet(Users user) throws JSONException {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + Constant.AUTHORIZATION);

        FlwWalletRequest payload = generatePayload(user);

        HttpEntity<FlwWalletRequest> request = new HttpEntity<>(payload, headers);

        FlwVirtualAccountResponse body = restTemplate.exchange(
                Constant.CREATE_VIRTUAL_ACCOUNT_API,
                HttpMethod.POST,
                request,
                FlwVirtualAccountResponse.class
        ).getBody();

        return Wallet.builder()
                .accountNumber(body.getData().getAccountNumber())
                .balance(BigDecimal.valueOf(0.00))
                .bankName(body.getData().getBankName())
                .createAt(LocalDateTime.now())
                .modifyAt(LocalDateTime.now())
                .build();
    }

    public void getToken(String token){
        this.userToken = token;
    }




    @Override
    public WalletDto viewWalletDetails() {

    WalletDto walletDto = new WalletDto();

    User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Users users = usersRepository.findUsersByEmail(user.getUsername());
    if(users == null){
        throw new UsernameNotFoundException("User Not found");
    }
    Wallet wallet = walletRepository.findWalletByUsers(users.getWallet().getUsers());
        BeanUtils.copyProperties(wallet,walletDto);
        return walletDto;
    }

    @Override
    public BaseResponse<String> fundWallet(
            AccountFundDTO amount) {

        if(amount.getAmount().compareTo(BigDecimal.ONE) < 0){
            return new BaseResponse<>(HttpStatus.BAD_REQUEST, "Invalid Amount", null);
        }
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users users = usersRepository.findUsersByEmail(user.getUsername());
        Wallet wallet = walletRepository.findWalletByUsers(users);
        BigDecimal bigDecimal = wallet.getBalance().add(amount.getAmount());
        wallet.setBalance(bigDecimal);
        walletRepository.save(wallet);
        return new BaseResponse<>(HttpStatus.OK, "Account has been funded successfully", null);


    }

    private FlwWalletRequest generatePayload(Users user) {
        FlwWalletRequest jsono = new FlwWalletRequest();
        jsono.setEmail(user.getEmail());
        jsono.set_permanent(true);
        jsono.setBvn(user.getBVN());
        jsono.setPhonenumber(user.getPhoneNumber());
        jsono.setFirstname(user.getFirstName());
        jsono.setLastname(user.getLastName());
        jsono.setTx_ref("fintech app");
        jsono.setNarration("fintech");

        return jsono;
    }


}
