/* =========================================================
   Selecteur d'emojis du client mobile

   Bibliotheque : emoji-picker-element (equivalent sans React de
   emoji-picker-react, utilise par le client bureau).
   Les fichiers de la bibliotheque et la liste des emojis en francais
   sont copies dans lib/ : le selecteur marche sans acces a Internet.

   L'emoji est insere dans le champ de saisie, a l'endroit du curseur.
   On declenche ensuite l'evenement "input" : app.js met donc a jour
   le bouton Envoyer et envoie le signal "en train d'ecrire",
   exactement comme si l'emoji avait ete tape au clavier.
   ========================================================= */

import Picker from "./lib/emoji-picker-element/picker.js";
import fr from "./lib/emoji-picker-element/fr.js";

const champ = document.getElementById("champ-message");
const bouton = document.getElementById("btn-emoji");
const panneau = document.getElementById("panneau-emoji");

const selecteur = new Picker({
  locale: "fr",
  i18n: fr,
  dataSource: "lib/emoji-picker-element/data-fr.json",
});
selecteur.classList.add("selecteur-emoji");
panneau.appendChild(selecteur);

function afficherPanneau(ouvert) {
  panneau.hidden = !ouvert;
  bouton.setAttribute("aria-expanded", String(ouvert));
}

bouton.addEventListener("click", () => afficherPanneau(panneau.hidden));

selecteur.addEventListener("emoji-click", (evenement) => {
  const emoji = evenement.detail.unicode;
  if (!emoji || champ.disabled) return;

  // Meme limite que le serveur : 2000 caracteres maximum
  if (champ.value.length + emoji.length > champ.maxLength) return;

  const debut = champ.selectionStart ?? champ.value.length;
  const fin = champ.selectionEnd ?? champ.value.length;
  champ.value = champ.value.slice(0, debut) + emoji + champ.value.slice(fin);
  const position = debut + emoji.length;
  champ.setSelectionRange(position, position);

  champ.dispatchEvent(new Event("input", { bubbles: true }));
});

// Le panneau se ferme a l'envoi, au retour a la liste, ou en touchant ailleurs
document.getElementById("form-message").addEventListener("submit", () => afficherPanneau(false));
document.getElementById("btn-retour").addEventListener("click", () => afficherPanneau(false));
document.addEventListener("click", (evenement) => {
  if (!panneau.hidden && !panneau.contains(evenement.target) && !bouton.contains(evenement.target)) {
    afficherPanneau(false);
  }
});
