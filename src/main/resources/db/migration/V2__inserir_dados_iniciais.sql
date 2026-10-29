INSERT INTO setor (nome, sigla) VALUES
('Coordenação de Tecnologia da Informação e Comunicação', 'COTIC'),
('Coordenação de Atividades de Serviços e Manutenção', 'COAS');

INSERT INTO categoria (nome, descricao, setor_id) VALUES
('Rede e Wi-Fi', 'Problemas com conexão de rede cabeada ou sem fio', 1),
('Equipamentos e Projetores', 'Suporte a projetores, computadores de lab e monitores', 1),
('Manutenção Elétrica', 'Problemas com tomadas, lâmpadas e disjuntores', 2),
('Manutenção Predial e Hidráulica', 'Vazamentos, fechaduras, portas e reformas', 2);

INSERT INTO usuario (nome, email, matricula, tipo, setor_id) VALUES
('Alice Silva', 'alice.silva@ifrn.edu.br', '20261001', 'SOLICITANTE', NULL),
('Bruno Souza', 'bruno.souza@ifrn.edu.br', '20261002', 'SOLICITANTE', NULL),
('Carlos TI', 'carlos.ti@ifrn.edu.br', '20262001', 'TECNICO', 1),
('Daniel Infra', 'daniel.infra@ifrn.edu.br', '20262002', 'TECNICO', 2);
