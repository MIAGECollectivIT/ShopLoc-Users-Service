# 🚀 Collection Bruno - Users Service API (ShopLoc)

Cette collection de tests fonctionnels et d'intégration HTTP couvre les endpoints du microservice **Users Service** (Authentification et Supervision Administrative).

---

## 📁 Structure de la Collection

```
bruno/Users Service/
├── bruno.json
├── environments/
│   ├── Local.bru              # Configuration pour localhost:8080
│   └── Dev.bru                # Configuration pour https://dev.shop-loc.fr
│
├── 00 - Authentification/     # Connexion automatique pour alimenter les tokens
│   ├── 01 - Connexion Administrateur (Marius) - 200.bru
│   ├── 02 - Connexion Commercant (Suzanne) - 200.bru
│   └── 03 - Connexion Client (Pierre) - 200.bru
│
└── 01 - Supervision Utilisateurs (Admin - SLE-68)/ # Endpoints d'administration et modération
    ├── 01 - Lister les utilisateurs avec pagination et recherche (ADMIN) - 200.bru
    ├── 02 - Filtrer les utilisateurs par role et statut verrouille (ADMIN) - 200.bru
    ├── 03 - Consulter un utilisateur par ID public (ADMIN) - 200.bru
    ├── 04 - Mettre a jour un utilisateur - Promotion COMMERCANT (ADMIN) - 200.bru
    ├── 05 - Deverrouiller un compte utilisateur (ADMIN) - 200.bru
    ├── 06 - Rejet acces supervision par role CLIENT - 403.bru
    ├── 07 - Rejet acces supervision par role COMMERCANT - 403.bru
    └── 08 - Rejet acces supervision sans token - 401.bru
```

---

## 🛠️ Utilisation dans Bruno

1. Ouvrez l'application **Bruno**.
2. Cliquez sur **Open Collection** et sélectionnez le dossier `bruno/Users Service`.
3. En haut à droite, sélectionnez l'environnement :
   - `Local` pour pointer vers `http://localhost:8080`.
   - `Dev` pour pointer vers `https://dev.shop-loc.fr`.
4. Ouvrez le dossier **`00 - Authentification`**, remplacez `USER` et `PASSWORD` par vos identifiants dans la requête choisie (Admin, Commerçant ou Client) et cliquez sur **Send** : le token JWT est **automatiquement injecté** dans les variables d'environnement (`adminToken`, `merchantToken`, `clientToken`).
5. Cliquez sur **Run** sur le dossier **`01 - Supervision Utilisateurs (Admin - SLE-68)`** pour exécuter les tests fonctionnels avec assertions automatisées (`res.status`, `res.body`).

> [!IMPORTANT]
> **Sécurité des identifiants & Git (`skip-worktree`) :**
> Les fichiers de requêtes d'authentification contiennent par défaut `"email": "USER"` et `"password": "PASSWORD"` dans le dépôt distant.
> Pour éviter que vos identifiants locaux ne soient commités par erreur :
> ```bash
> git update-index --skip-worktree "bruno/Users Service/00 - Authentification/"*
> ```

