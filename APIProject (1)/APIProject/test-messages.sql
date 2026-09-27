
USE messagerie;
INSERT INTO messages (expediteur_id, destinataire_id, contenu, date_envoi) VALUES
  (1, 2, 'Heyyy gurl !',            NOW() - INTERVAL 2 MINUTE),
  (2, 1, 'Hello Jennyba 💁🏾‍♀️, ca va ?',   NOW() - INTERVAL 1 MINUTE),
  (1, 2, 'Oui et toi 🙂?',          NOW());
