package com.hemorede.service;

import com.hemorede.domain.enums.Perfil;
import com.hemorede.domain.model.Doador;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.Usuario;
import com.hemorede.dto.CadastroDoadorRequest;
import com.hemorede.dto.NovoUsuarioRequest;
import com.hemorede.dto.SessaoResponse;
import com.hemorede.dto.UsuarioResponse;
import com.hemorede.exception.ConflitoCadastroException;
import com.hemorede.exception.CredenciaisInvalidasException;
import com.hemorede.repository.DoadorRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.UsuarioRepository;
import com.hemorede.seguranca.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Login, autocadastro de doador e contas criadas pelo hemocentro. */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final DoadorRepository doadorRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar contas. */
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarioRepository, DoadorRepository doadorRepository,
                               HospitalRepository hospitalRepository, PasswordEncoder passwordEncoder,
                               TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.doadorRepository = doadorRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFicticio = passwordEncoder.encode("conta-inexistente-" + System.nanoTime());
    }

    @Transactional(readOnly = true)
    public SessaoResponse login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizar(email)).orElse(null);
        boolean senhaOk = passwordEncoder.matches(senha, usuario != null ? usuario.getSenhaHash() : hashFicticio);
        if (usuario == null || !senhaOk || !usuario.isAtivo()) {
            throw new CredenciaisInvalidasException();
        }
        return sessao(usuario);
    }

    @Transactional
    public SessaoResponse cadastrarDoador(CadastroDoadorRequest req) {
        String email = normalizar(req.email());
        String digitos = req.cpf().replaceAll("\\D", "");
        // mesmo formato dos cadastros existentes (000.000.000-00)
        String cpf = digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoCadastroException("Já existe uma conta com este e-mail.");
        }
        if (doadorRepository.existsByCpf(cpf)) {
            throw new ConflitoCadastroException("Já existe um doador com este CPF.");
        }
        Doador doador = new Doador(null, req.nome().trim(), cpf, req.tipoSanguineo(), null, new ArrayList<>());
        doador.setSexo(req.sexo());
        doador.setDataNascimento(req.dataNascimento());
        doador.setPesoKg(req.pesoKg());
        doador.setTelefone(req.telefone());
        doador = doadorRepository.save(doador);

        Usuario usuario = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()), Perfil.DOADOR);
        usuario.setDoador(doador);
        return sessao(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse criarUsuario(NovoUsuarioRequest req) {
        String email = normalizar(req.email());
        if (req.perfil() == Perfil.DOADOR) {
            throw new IllegalStateException("Contas de doador são criadas pelo próprio doador em Criar conta.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoCadastroException("Já existe uma conta com este e-mail.");
        }
        Usuario usuario = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()), req.perfil());
        if (req.perfil() == Perfil.HOSPITAL) {
            if (req.hospitalId() == null) {
                throw new IllegalStateException("Informe o hospital da conta HOSPITAL.");
            }
            Hospital hospital = hospitalRepository.findById(req.hospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital não encontrado: " + req.hospitalId()));
            usuario.setHospital(hospital);
        }
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public SessaoResponse sessaoAtual(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isAtivo)
                .map(u -> new SessaoResponse(null, null, u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                        u.getHospital() != null ? u.getHospital().getId() : null,
                        u.getHospital() != null ? u.getHospital().getNome() : null,
                        u.getDoador() != null ? u.getDoador().getId() : null))
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    private SessaoResponse sessao(Usuario u) {
        TokenService.TokenEmitido token = tokenService.emitir(u);
        return new SessaoResponse(token.token(), token.expiraEm(), u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getHospital() != null ? u.getHospital().getId() : null,
                u.getHospital() != null ? u.getHospital().getNome() : null,
                u.getDoador() != null ? u.getDoador().getId() : null);
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
