package com.example.coffeeloyalty.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.example.coffeeloyalty.dto.CustomerForm;
import com.example.coffeeloyalty.dto.PurchaseForm;
import com.example.coffeeloyalty.dto.RedemptionForm;
import com.example.coffeeloyalty.entity.Customer;
import com.example.coffeeloyalty.entity.Purchase;
import com.example.coffeeloyalty.entity.Redemption;
import com.example.coffeeloyalty.entity.Reward;
import com.example.coffeeloyalty.repository.CustomerRepository;
import com.example.coffeeloyalty.repository.PurchaseRepository;
import com.example.coffeeloyalty.repository.RedemptionRepository;
import com.example.coffeeloyalty.repository.RewardRepository;
import com.example.coffeeloyalty.service.CustomerService;
import com.example.coffeeloyalty.service.PurchaseService;
import com.example.coffeeloyalty.service.RedemptionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebFlowIntegrationTest {

    private final MockMvcTester mockMvc;
    private final CustomerService customerService;
    private final PurchaseService purchaseService;
    private final RedemptionService redemptionService;
    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;

    private Reward americano;
    private Reward dessert;
    private Reward combo;

    @Autowired
    WebFlowIntegrationTest(MockMvcTester mockMvc, CustomerService customerService, PurchaseService purchaseService,
            RedemptionService redemptionService, CustomerRepository customerRepository,
            PurchaseRepository purchaseRepository, RedemptionRepository redemptionRepository,
            RewardRepository rewardRepository) {
        this.mockMvc = mockMvc;
        this.customerService = customerService;
        this.purchaseService = purchaseService;
        this.redemptionService = redemptionService;
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardRepository = rewardRepository;
    }

    @BeforeEach
    void setUp() {
        clean();
        americano = rewardRepository.save(
                new Reward("Café americano", "Un café americano de la casa.", 10, true));
        rewardRepository.save(new Reward("Cappuccino", "Un cappuccino de la casa.", 20, true));
        dessert = rewardRepository.save(
                new Reward("Postre del día", "Una porción del postre del día.", 30, true));
        combo = rewardRepository.save(
                new Reward("Combo de café", "Un café y un postre a elección.", 50, true));
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void updatesCustomerNameAndNormalizesEmailWithoutTouchingBalanceOrJoinDate() {
        Customer customer = customerService.create(customerForm("Nombre Original", "original@example.com"));
        registerPurchase(customer, "9.99");
        Customer before = customerService.findById(customer.getId());

        assertThat(mockMvc.post().uri("/customers/" + customer.getId())
                .param("name", "Nombre Nuevo")
                .param("email", "NUEVO.CORREO@EXAMPLE.COM"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers")
                .flash().containsEntry("message", "Cliente actualizado correctamente.");

        Customer updated = customerService.findById(customer.getId());
        assertThat(updated.getName()).isEqualTo("Nombre Nuevo");
        assertThat(updated.getEmail()).isEqualTo("nuevo.correo@example.com");
        assertThat(updated.getPoints()).isEqualTo(before.getPoints());
        assertThat(updated.getJoinDate()).isEqualTo(before.getJoinDate());
    }

    @Test
    void rejectsInvalidCustomerEditAndKeepsStoredData() {
        Customer customer = customerService.create(customerForm("Nombre Original", "original@example.com"));

        assertThat(mockMvc.post().uri("/customers/" + customer.getId())
                .param("name", "")
                .param("email", "no-es-un-email"))
                .hasStatusOk()
                .bodyText()
                .contains("El nombre es obligatorio.")
                .contains("El email no es válido.");

        Customer unchanged = customerService.findById(customer.getId());
        assertThat(unchanged.getName()).isEqualTo("Nombre Original");
        assertThat(unchanged.getEmail()).isEqualTo("original@example.com");
    }

    @Test
    void rejectsCustomerEditWithAnotherCustomersEmailInDifferentCase() {
        Customer other = customerService.create(customerForm("Otro Cliente", "otro@example.com"));
        Customer customer = customerService.create(customerForm("Cliente Editado", "cliente.editado@example.com"));

        assertThat(mockMvc.post().uri("/customers/" + customer.getId())
                .param("name", "Cliente Editado")
                .param("email", "OTRO@EXAMPLE.COM"))
                .hasStatusOk()
                .bodyText().contains("Ya existe un cliente con el email " + other.getEmail() + ".");

        Customer unchanged = customerService.findById(customer.getId());
        assertThat(unchanged.getEmail()).isEqualTo("cliente.editado@example.com");
        assertThat(customerRepository.count()).isEqualTo(2);
    }

    @Test
    void deletesCustomerWithItsPurchasesAndRedemptionsButKeepsRewards() {
        Customer customer = customerService.create(customerForm("Cliente Eliminado", "eliminado@example.com"));
        registerPurchase(customer, "9.99");
        redeem(customer, americano);

        assertThat(mockMvc.post().uri("/customers/" + customer.getId() + "/delete"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers")
                .flash().containsEntry("message", "Cliente eliminado correctamente.");

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(rewardRepository.findAll()).hasSize(4);
        assertThat(mockMvc.get().uri("/customers/" + customer.getId() + "/edit"))
                .hasStatus(404)
                .bodyText().contains("No existe el cliente con id " + customer.getId() + ".");
    }

    @Test
    void preselectsTheRequestedCustomerOnThePurchaseForm() {
        Customer customer = customerService.create(customerForm("Cliente Preseleccionado", "preseleccionado@example.com"));

        assertThat(mockMvc.get().uri("/purchases/new").param("customerId", customer.getId().toString()))
                .hasStatusOk()
                .body().asString().containsPattern(selectedOption(customer.getId()));
    }

    @Test
    void doesNotPreselectAnyCustomerForAnUnknownPurchaseCustomerId() {
        customerService.create(customerForm("Cliente Existente", "existente@example.com"));

        assertThat(mockMvc.get().uri("/purchases/new").param("customerId", "999999"))
                .hasStatusOk()
                .body().asString().doesNotContain("selected=\"selected\"");
    }

    @Test
    void preselectsCustomerAndRewardOnTheRedemptionForm() {
        Customer customer = customerService.create(customerForm("Cliente Canje", "cliente.canje@example.com"));

        assertThat(mockMvc.get().uri("/redemptions/new")
                .param("rewardId", dessert.getId().toString())
                .param("customerId", customer.getId().toString()))
                .hasStatusOk()
                .body().asString()
                .containsPattern(selectedOption(customer.getId()))
                .containsPattern(selectedOption(dessert.getId()));
    }

    @Test
    void doesNotPreselectAnythingForUnknownRedemptionParameters() {
        Customer customer = customerService.create(customerForm("Cliente Existente", "existente@example.com"));

        assertThat(mockMvc.get().uri("/redemptions/new")
                .param("rewardId", "999999")
                .param("customerId", "999999"))
                .hasStatusOk()
                .body().asString().doesNotContain("selected=\"selected\"");
    }

    @Test
    void rejectsPurchaseWithEmptyAmount() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", ""))
                .hasStatusOk()
                .bodyText().contains("El monto es obligatorio.");

        assertThat(purchaseRepository.count()).isZero();
        assertThat(customerService.findById(customer.getId()).getPoints()).isZero();
    }

    @Test
    void rejectsPurchaseWithZeroAmount() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "0"))
                .hasStatusOk()
                .bodyText().contains("El monto debe ser mayor que cero.");

        assertThat(purchaseRepository.count()).isZero();
        assertThat(customerService.findById(customer.getId()).getPoints()).isZero();
    }

    @Test
    void rejectsPurchaseAboveTheMaximumAmount() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "1000.01"))
                .hasStatusOk()
                .bodyText().contains("El monto no puede superar $1,000.00.");

        assertThat(purchaseRepository.count()).isZero();
        assertThat(customerService.findById(customer.getId()).getPoints()).isZero();
    }

    @Test
    void rejectsPurchaseWithMoreThanTwoDecimals() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "12.345"))
                .hasStatusOk()
                .bodyText().contains("El monto admite hasta 2 decimales.");

        assertThat(purchaseRepository.count()).isZero();
        assertThat(customerService.findById(customer.getId()).getPoints()).isZero();
    }

    @Test
    void acceptsTheMaximumPurchaseAmount() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "1000.00"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId());

        List<Purchase> purchases = purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        assertThat(purchases).hasSize(1);
        assertThat(purchases.get(0).getAmount()).isEqualByComparingTo("1000.00");
        assertThat(purchases.get(0).getPointsEarned()).isEqualTo(1000);
        assertThat(customerService.findById(customer.getId()).getPoints()).isEqualTo(1000);
    }

    @Test
    void acceptsAnAmountBelowFiftyCentsWithoutAddingPoints() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));
        registerPurchase(customer, "25.00");

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "0.49"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId());

        List<Purchase> purchases = purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        assertThat(purchases).hasSize(2);
        assertThat(purchases)
                .filteredOn(purchase -> purchase.getAmount().compareTo(new BigDecimal("0.49")) == 0)
                .singleElement()
                .satisfies(purchase -> assertThat(purchase.getPointsEarned()).isZero());
        assertThat(customerService.findById(customer.getId()).getPoints()).isEqualTo(25);
    }

    @Test
    void doesNotExposeTechnicalMessagesForANonNumericPurchaseAmount() {
        Customer customer = customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        MvcTestResult result = mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "abc")
                .exchange();

        assertThat(result.getResponse().getStatus()).isIn(200, 400);
        assertThat(result).bodyText()
                .doesNotContain("Failed to convert")
                .doesNotContain("NumberFormatException")
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .contains("El monto debe ser un número válido.");
    }

    @Test
    void doesNotExposeTechnicalMessagesForANonNumericPurchaseCustomerId() {
        customerService.create(customerForm("Cliente Compra", "cliente.compra@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", "abc")
                .param("amount", "9.99"))
                .hasStatusOk()
                .bodyText()
                .contains("Seleccione un cliente.")
                .doesNotContain("Failed to convert")
                .doesNotContain("NumberFormatException")
                .doesNotContain("Exception")
                .doesNotContain("org.springframework");

        assertThat(purchaseRepository.count()).isZero();
    }

    @Test
    void doesNotExposeTechnicalMessagesForNonNumericRedemptionParameters() {
        customerService.create(customerForm("Cliente Canje", "cliente.canje@example.com"));

        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", "abc")
                .param("rewardId", "xyz"))
                .hasStatusOk()
                .bodyText()
                .contains("Seleccione un cliente.")
                .contains("Seleccione una recompensa.")
                .doesNotContain("Failed to convert")
                .doesNotContain("NumberFormatException")
                .doesNotContain("Exception")
                .doesNotContain("org.springframework");

        assertThat(redemptionRepository.count()).isZero();
    }

    @Test
    void showsTheExactSuccessMessageAfterAPurchase() {
        Customer customer = customerService.create(customerForm("Cliente Mensaje", "cliente.mensaje@example.com"));

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", customer.getId().toString())
                .param("amount", "9.99"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId())
                .flash().containsEntry("message",
                        "Compra registrada correctamente. Puntos obtenidos: 10. Saldo actual: 10.");
    }

    @Test
    void showsTheExactSuccessMessageAfterARedemption() {
        Customer customer = customerService.create(customerForm("Cliente Mensaje", "cliente.mensaje@example.com"));
        registerPurchase(customer, "50.00");

        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", customer.getId().toString())
                .param("rewardId", dessert.getId().toString()))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + customer.getId())
                .flash().containsEntry("message",
                        "Canje registrado correctamente. Recompensa: Postre del día. "
                                + "Puntos gastados: 30. Saldo actual: 20.");
    }

    @Test
    void walksScenarioAThroughWebRequestsOnly() {
        assertThat(mockMvc.post().uri("/customers")
                .param("name", "Sebastián")
                .param("email", "sebastian@example.com"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers");

        Customer sebastian = customerWithEmail("sebastian@example.com");

        assertThat(mockMvc.post().uri("/purchases")
                .param("customerId", sebastian.getId().toString())
                .param("amount", "50.99"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + sebastian.getId());
        assertThat(customerService.findById(sebastian.getId()).getPoints()).isEqualTo(51);

        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", sebastian.getId().toString())
                .param("rewardId", dessert.getId().toString()))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/points/" + sebastian.getId());
        assertThat(customerService.findById(sebastian.getId()).getPoints()).isEqualTo(21);
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(sebastian.getId())).hasSize(1);

        assertThat(mockMvc.post().uri("/redemptions")
                .param("customerId", sebastian.getId().toString())
                .param("rewardId", combo.getId().toString()))
                .hasStatusOk()
                .bodyText()
                .contains("Sebastián tiene 21 puntos y la recompensa Combo de café requiere 50.");
        assertThat(customerService.findById(sebastian.getId()).getPoints()).isEqualTo(21);
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(sebastian.getId())).hasSize(1);

        assertThat(mockMvc.get().uri("/points/" + sebastian.getId()))
                .hasStatusOk()
                .bodyText().contains("21 puntos");
    }

    @Test
    void keepsHistoryAfterEditingEmailAndDeletesEverythingWhenRemovingTheCustomer() {
        Customer customer = customerService.create(customerForm("Cliente Historial", "historial@example.com"));
        registerPurchase(customer, "50.00");
        registerPurchase(customer, "10.00");
        redeem(customer, americano);

        List<Long> purchaseIds = purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(Purchase::getId)
                .toList();
        List<Long> redemptionIds = redemptionRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(Redemption::getId)
                .toList();

        assertThat(mockMvc.post().uri("/customers/" + customer.getId())
                .param("name", "Cliente Historial")
                .param("email", "CORREGIDO@EXAMPLE.COM"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers");

        assertThat(customerService.findById(customer.getId()).getEmail()).isEqualTo("corregido@example.com");
        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()))
                .extracting(Purchase::getId)
                .containsExactlyInAnyOrderElementsOf(purchaseIds);
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()))
                .extracting(Redemption::getId)
                .containsExactlyInAnyOrderElementsOf(redemptionIds);

        assertThat(mockMvc.post().uri("/customers/" + customer.getId() + "/delete"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/customers");

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
        assertThat(purchaseRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(redemptionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
        assertThat(rewardRepository.findAll()).hasSize(4);
    }

    private Customer customerWithEmail(String email) {
        return customerService.findAll().stream()
                .filter(customer -> email.equals(customer.getEmail()))
                .findFirst()
                .orElseThrow();
    }

    private Pattern selectedOption(Long id) {
        return Pattern.compile(
                "<option(?=[^>]*value=\"" + id + "\")(?=[^>]*selected=\"selected\")[^>]*>");
    }

    private void registerPurchase(Customer customer, String amount) {
        PurchaseForm form = new PurchaseForm();
        form.setCustomerId(customer.getId());
        form.setAmount(new BigDecimal(amount));
        purchaseService.register(form);
    }

    private void redeem(Customer customer, Reward reward) {
        RedemptionForm form = new RedemptionForm();
        form.setCustomerId(customer.getId());
        form.setRewardId(reward.getId());
        redemptionService.redeem(form);
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
