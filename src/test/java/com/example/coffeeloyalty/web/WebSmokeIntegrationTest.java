package com.example.coffeeloyalty.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSmokeIntegrationTest {

    private final MockMvcTester mockMvc;
    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;

    private Customer customer;
    private Reward reward;

    @Autowired
    WebSmokeIntegrationTest(MockMvcTester mockMvc, CustomerService customerService, PurchaseService purchaseService,
            CustomerRepository customerRepository, PurchaseRepository purchaseRepository,
            RedemptionRepository redemptionRepository, RewardRepository rewardRepository) {
        this.mockMvc = mockMvc;
        this.customerService = customerService;
        this.purchaseService = purchaseService;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardRepository = rewardRepository;
    }

    @BeforeEach
    void setUp() {
        clean();
        customer = customerService.create(customerForm("Cliente Humo", "cliente.humo@example.com"));
        reward = rewardRepository.save(new Reward("Café americano", "Un café americano de la casa.", 10, true));
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void rendersTheHomePage() {
        assertThat(mockMvc.get().uri("/"))
                .hasStatusOk()
                .bodyText().contains("acumulan puntos con cada compra");
    }

    @Test
    void rendersTheCustomerList() {
        assertThat(mockMvc.get().uri("/customers"))
                .hasStatusOk()
                .bodyText().contains("Fecha de incorporación");
    }

    @Test
    void rendersTheNewCustomerForm() {
        assertThat(mockMvc.get().uri("/customers/new"))
                .hasStatusOk()
                .bodyText().contains("Nuevo cliente");
    }

    @Test
    void rendersThePurchaseList() {
        assertThat(mockMvc.get().uri("/purchases"))
                .hasStatusOk()
                .bodyText().contains("No hay compras registradas.");
    }

    @Test
    void rendersTheNewPurchaseForm() {
        assertThat(mockMvc.get().uri("/purchases/new"))
                .hasStatusOk()
                .bodyText().contains("Monto (USD)");
    }

    @Test
    void rendersThePointsList() {
        assertThat(mockMvc.get().uri("/points"))
                .hasStatusOk()
                .bodyText().contains("Ver detalle");
    }

    @Test
    void rendersTheRewardsCatalog() {
        assertThat(mockMvc.get().uri("/rewards"))
                .hasStatusOk()
                .bodyText().contains("Costo");
    }

    @Test
    void rendersTheRedemptionList() {
        assertThat(mockMvc.get().uri("/redemptions"))
                .hasStatusOk()
                .bodyText().contains("No hay canjes registrados.");
    }

    @Test
    void rendersTheNewRedemptionForm() {
        assertThat(mockMvc.get().uri("/redemptions/new"))
                .hasStatusOk()
                .bodyText().contains("Seleccione una recompensa");
    }

    @Test
    void rendersTheCustomerEditForm() {
        assertThat(mockMvc.get().uri("/customers/" + customer.getId() + "/edit"))
                .hasStatusOk()
                .bodyText().contains("Cliente Humo");
    }

    @Test
    void rendersTheCustomerPointsDetail() {
        assertThat(mockMvc.get().uri("/points/" + customer.getId()))
                .hasStatusOk()
                .bodyText().contains("Cliente Humo");
    }

    @Test
    void returnsNotFoundWhenEditingAnUnknownCustomer() {
        assertThat(mockMvc.get().uri("/customers/999999/edit"))
                .hasStatus(404)
                .bodyText().contains("No existe el cliente con id 999999.");
    }

    @Test
    void returnsNotFoundWhenViewingUnknownCustomerPoints() {
        assertThat(mockMvc.get().uri("/points/999999"))
                .hasStatus(404)
                .bodyText().contains("No existe el cliente con id 999999.");
    }

    @Test
    void returnsNotFoundForAnUnknownRoute() {
        assertThat(mockMvc.get().uri("/ruta-inexistente")).hasStatus(404);
    }

    @Test
    void rejectsACustomerFormWithBlankNameAndInvalidEmail() {
        assertThat(mockMvc.post().uri("/customers")
                .param("name", "")
                .param("email", "no-es-un-email"))
                .hasStatusOk()
                .bodyText()
                .contains("El nombre es obligatorio.")
                .contains("El email no es válido.");
    }

    @Test
    void redirectsToTheCustomerListWhenCreatingAValidCustomer() {
        assertThat(mockMvc.post().uri("/customers")
                .param("name", "Cliente Válido")
                .param("email", "cliente.valido@example.com"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers");
    }

    @Test
    void rejectsACustomerWithAnEmailThatAlreadyExistsInDifferentCase() {
        assertThat(mockMvc.post().uri("/customers")
                .param("name", "Otro Cliente")
                .param("email", "CLIENTE.HUMO@EXAMPLE.COM"))
                .hasStatusOk()
                .bodyText().contains("Ya existe un cliente con el email cliente.humo@example.com.");

        assertThat(customerService.findAll())
                .filteredOn(existing -> "cliente.humo@example.com".equalsIgnoreCase(existing.getEmail()))
                .hasSize(1);
    }

    @Test
    void rejectsAPurchaseWithZeroAmount() {
        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "0"))
                .hasStatusOk()
                .bodyText().contains("El monto debe ser mayor que cero.");
    }

    @Test
    void redirectsToCustomerPointsAfterAPurchaseAndAddsTheRoundedPoints() {
        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "9.99"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId());

        assertThat(customerService.findById(customer.getId()).getPoints()).isEqualTo(10);
    }

    @Test
    void rejectsARedemptionWithoutCustomerOrReward() {
        assertThat(mockMvc.post().uri("/redemptions"))
                .hasStatusOk()
                .bodyText()
                .contains("Seleccione un cliente.")
                .contains("Seleccione una recompensa.");
    }

    @Test
    void redirectsToCustomerPointsAfterAValidRedemption() {
        registerPurchase(customer, "50.00");

        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", customer.getId().toString())
                .param("rewardId", reward.getId().toString()))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId());

        assertThat(customerService.findById(customer.getId()).getPoints()).isEqualTo(40);
    }

    @Test
    void rejectsARedemptionWhenTheBalanceIsTooLow() {
        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", customer.getId().toString())
                .param("rewardId", reward.getId().toString()))
                .hasStatusOk()
                .bodyText().contains("Cliente Humo tiene 0 puntos y la recompensa Café americano requiere 10.");

        assertThat(customerService.findById(customer.getId()).getPoints()).isZero();
    }

    private void registerPurchase(Customer customer, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customer.getId());
        form.setAmount(new BigDecimal(amount));
        purchaseService.register(form);
    }

    private CustomerForm customerForm(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }

    private void clean() {
        redemptionRepository.deleteAll();
        purchaseRepository.deleteAll();
        customerRepository.deleteAll();
        rewardRepository.deleteAll();
    }
}
